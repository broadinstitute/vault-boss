/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.boss.http.service;

import org.apache.commons.lang.ArrayUtils;
import org.genomebridge.boss.http.config.MessageConfiguration;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.db.ObjectRow;
import org.genomebridge.boss.http.models.ObjectCore;
import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.skife.jdbi.v2.DBI;

import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

/**
 * Implements all application logic and interacts with object store and DB.
 */
public class DatabaseBossAPI implements BossAPI {

    public DatabaseBossAPI( DBI dbi, ObjectStore localStore, ObjectStore cloudStore,  MessageConfiguration messages) {
        mDBI = dbi;
        mLocalStore = localStore;
        mCloudStore = cloudStore;
        messagesConf = messages;
    }

    @Override
    public ErrorDesc getObject(String objectId, String userName, ObjectDesc desc) {
        BossDAO dao = getDao();
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));
        ObjectRow rec = dao.findObjectById(objectId);
        if ( rec == null )
            return notFoundErr(objectId);
        if ( !"Y".equals(rec.active) )
            return goneErr(objectId);
        if ( !dao.canRead(objectId,userName) )
            return permsErr(objectId,userName,messagesConf.get("read"));
        rowToDesc(rec,desc,dao);
        return null;
    }

    @Override
    public ErrorDesc findObjectsByName(String objectName, String userName, List<ObjectDesc> descs) {
        descs.clear();
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));

        BossDAO dao = getDao();
        List<ObjectRow> recs = dao.findObjectsByName(userName, objectName);
        if ( recs == null || recs.size() == 0 )
            return new ErrorDesc(Response.Status.NOT_FOUND,String.format(messagesConf.get("noReadable"),objectName));

        for ( ObjectRow rec : recs ) {
            ObjectDesc desc = new ObjectDesc();
            rowToDesc(rec,desc,dao);
            descs.add(desc);
        }
        return null;
    }

    @Override
    public ErrorDesc insertObject(ObjectDesc rec, String userName) {
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));
        String errMsg = testCreationValidity(rec);
        if ( errMsg != null )
            return badReqErr(errMsg);

        rec.objectId = UUID.randomUUID().toString();

        // Use the location passed in by the user if the Object is an opaqueURI object,
        // otherwise generate a new (fresh) location.
        String loc = rec.storagePlatform.equals(StoragePlatform.OPAQUEURI.getValue()) ?
                rec.directoryPath : createLocation(rec);

        if ( rec.sizeEstimateBytes == null )
            rec.sizeEstimateBytes = gDefaultEstSize;

        List<String> readers = uniqueUsers(rec.readers);
        List<String> writers = uniqueUsers(rec.writers);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        BossDAO dao = getDao();
        dao.begin();
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes,
                loc, rec.storagePlatform, userName, now);
        dao.insertReaders(rec.objectId, readers);
        dao.insertWriters(rec.objectId, writers);
        dao.commit();
        return null;
    }

    @Override
    public ErrorDesc updateObject(ObjectDesc desc, String objectId, String userName) {
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));
        BossDAO dao = getDao();
        ObjectRow rec = dao.findObjectById(objectId);
        if ( rec == null )
            return notFoundErr(objectId);
        if ( !"Y".equals(rec.active) )
            return goneErr(objectId);
        if ( !dao.canWrite(objectId,userName) )
            return permsErr(objectId,userName,messagesConf.get("write"));

        String errMsg = testUpdateValidity(rec,desc);
        if ( errMsg != null )
            return badReqErr(errMsg);

        if ( desc.ownerId != null )
            rec.ownerId = desc.ownerId;

        List<String> readersToInsert = null;
        List<String> readersToDelete = null;
        if ( desc.readers != null ) {
            List<String> newUsers = Arrays.asList(desc.readers);
            List<String> curUsers = dao.findReadersById(desc.objectId);
            readersToInsert = diff(newUsers,curUsers);
            readersToDelete = diff(curUsers,newUsers);
        }
        List<String> writersToInsert = null;
        List<String> writersToDelete = null;
        if ( desc.writers != null ) {
            List<String> newUsers = Arrays.asList(desc.writers);
            List<String> curUsers = dao.findWritersById(desc.objectId);
            writersToInsert = diff(newUsers,curUsers);
            writersToDelete = diff(curUsers,newUsers);
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        dao.begin();
        dao.updateObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, now);
        if ( readersToInsert != null )
            dao.insertReaders(rec.objectId, readersToInsert);
        if ( readersToDelete != null )
            dao.deleteReaders(rec.objectId, readersToDelete);
        if ( writersToInsert != null )
            dao.insertWriters(rec.objectId, writersToInsert);
        if ( writersToDelete != null )
            dao.deleteWriters(rec.objectId, writersToDelete);
        dao.commit();

        return null;
    }

    /*
        if this object resides in the object store, also delete from the object store.

        BOSS rev3 spec says: If BOSS is unable to delete the underlying object from the object storage
        (e.g. non-transient network failure, or other error from object store server), it will return an
        appropriate 50x error, and the entry for this object will not be deleted from BOSS.

        So, we delete from the db first, then the object store. If object store deletion fails,
        we rollback the db deletion.
    */
    @Override
    public ErrorDesc deleteObject(String objectId, String userName) {
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));
        BossDAO dao = getDao();
        ObjectRow rec = dao.findObjectById(objectId);
        if ( rec == null || !"Y".equals(rec.active) )
            return notFoundErr(objectId);
        if ( !dao.canWrite(objectId,userName) )
            return permsErr(objectId,userName,messagesConf.get("write"));

        ObjectStore store = getObjectStore(rec.storagePlatform);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.begin();

        // Try to remove object resource first so we don't end up with orphaned records.
        try {
            dao.deleteObject(rec.objectId, now);
        } catch (Exception e) {
            dao.rollback();
            return new ErrorDesc(Response.Status.INTERNAL_SERVER_ERROR,
                    messagesConf.get("unableDelete")+':'+e.getMessage());
        }

        // Only commit the ObjectResource delete after checking for ObjectStore deletion.
        try {
            if ( store != null && rec.directoryPath != null )
                store.deleteObject(rec.directoryPath);
            dao.commit();
        } catch (Exception e) {
            dao.rollback();
            return new ErrorDesc(Response.Status.INTERNAL_SERVER_ERROR,
                   messagesConf.get("unableDeleteFromObjectStore") +": "+e.getMessage());
        }

        return null;
    }

    @Override
    public ErrorDesc resolveObject(String objectId, String userName, ResolveRequest req, ResolveResponse resp) {
        if ( userName == null )
            return badReqErr(messagesConf.get("remoteUser"));
        BossDAO dao = getDao();
        ObjectRow rec = dao.findObjectById(objectId);
        if ( rec == null )
            return notFoundErr(objectId);
        if ( !"Y".equals(rec.active) )
            return goneErr(objectId);

        if ( req.httpMethod.equals(HttpMethod.PUT) ) {
            if ( !dao.canWrite(objectId,userName) )
                return permsErr(objectId,userName,messagesConf.get("write"));
        }
        else if ( req.httpMethod.equals(HttpMethod.GET) ||
                req.httpMethod.equals(HttpMethod.HEAD) ) {
            if ( !dao.canRead(objectId,userName) )
                return permsErr(objectId,userName,messagesConf.get("read"));
        }
        else
            return badReqErr(messagesConf.get("httpMethod"));

        String contentMD5x64 = null;
        if ( req.contentMD5Hex != null ) {
            if ( req.contentMD5Hex.length() != 32 )
                return badReqErr(messagesConf.get("md5"));
            try {
                contentMD5x64 = DatatypeConverter.printBase64Binary(DatatypeConverter.parseHexBinary(req.contentMD5Hex));
            }
            catch ( IllegalArgumentException e ) {
                return badReqErr(messagesConf.get("contentMD5"));
            }
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.updateResolveDate(objectId, now);

        resp.validityPeriodSeconds = req.validityPeriodSeconds;
        resp.contentType = req.contentType;
        resp.contentMD5Hex = req.contentMD5Hex;
        if ( rec.storagePlatform.equals(StoragePlatform.OPAQUEURI.getValue()) )
            resp.objectUrl = URI.create(rec.directoryPath);
        else {
            long timeout = now.getTime() + 1000L*req.validityPeriodSeconds;
            ObjectStore objStore = getObjectStore(rec.storagePlatform);
            resp.objectUrl = objStore.generatePresignedURL(rec.directoryPath, req.httpMethod,
                    timeout, req.contentType, contentMD5x64);
        }
        return null;
    }

    private BossDAO getDao() {
        return mDBI.onDemand(BossDAO.class);
    }

    private ObjectStore getObjectStore( String storagePlatform ) {
        if ( storagePlatform.equals(StoragePlatform.CLOUDSTORE.getValue()) )
            return mCloudStore;
        if ( storagePlatform.equals(StoragePlatform.LOCALSTORE.getValue()) )
            return mLocalStore;

        return null;
    }

    private static String testCreationValidity( ObjectDesc desc ) {
        StringBuilder sb = new StringBuilder();
        if ( desc.objectId != null ) add(sb,messagesConf.get("objectIdNotSupplied"));
        if ( desc.objectName == null ) add(sb,messagesConf.get("objectValidation"));
        if ( desc.ownerId == null ) add(sb,messagesConf.get("ownerIdValidation"));
        if ( desc.storagePlatform == null ) add(sb,messagesConf.get("storagePlatformValidation"));
        else {
            if ( desc.storagePlatform.equals(StoragePlatform.CLOUDSTORE.getValue()) ||
                    desc.storagePlatform.equals(StoragePlatform.LOCALSTORE.getValue()) ) {
                if ( desc.directoryPath != null )
                    add(sb,String.format(messagesConf.get("directoryPathNotSupplied"),desc.storagePlatform));
            }
            else if ( desc.storagePlatform.equals(StoragePlatform.OPAQUEURI.getValue()) ) {
                if ( desc.directoryPath == null )
                    add(sb,String.format(messagesConf.get("directoryPathToSupply"),StoragePlatform.OPAQUEURI.getValue()));
            }
            else {

                add(sb, String.format(messagesConf.get("storagePlatformOptions"),
                        StoragePlatform.CLOUDSTORE.getValue(),
                        StoragePlatform.LOCALSTORE.getValue(),
                        StoragePlatform.OPAQUEURI.getValue()));
            }
        }
        return sb.length() > 0 ? sb.append('.').toString() : null;
    }

    private static String testUpdateValidity( ObjectCore oldObj, ObjectCore newObj ) {
        StringBuilder sb = new StringBuilder();
        if ( !consistent(oldObj.objectId,newObj.objectId) )
            add(sb,messagesConf.get("objectIdFixed"));
        if ( !consistent(oldObj.objectName,newObj.objectName) )
            add(sb,messagesConf.get("objectNameFixed"));
        if ( !consistent(oldObj.storagePlatform,newObj.storagePlatform) )
            add(sb,messagesConf.get("storagePlatformFixed"));
        if ( !consistent(oldObj.sizeEstimateBytes,newObj.sizeEstimateBytes) )
            add(sb,messagesConf.get("sizeEstimateFixed"));
        if ( !consistent(oldObj.directoryPath,newObj.directoryPath) )
            add(sb,messagesConf.get("directoryPathFixed"));
        return sb.length() > 0 ? sb.append('.').toString() : null;
    }

    private static void add(StringBuilder sb, String message ) {
        if ( sb.length() > 0 ) sb.append(";/n");
        sb.append(message);
    }

    private static void rowToDesc( ObjectRow row, ObjectDesc desc, BossDAO dao ) {
        desc.copy(row);
        if ( !desc.storagePlatform.equals(StoragePlatform.OPAQUEURI.getValue()) )
            desc.directoryPath = null;
        desc.readers = dao.findReadersById(row.objectId).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        desc.writers = dao.findWritersById(row.objectId).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    private static List<String> uniqueUsers( String[] users ) {
        Set<String> userSet = new TreeSet<>(Arrays.asList(users));
        return new ArrayList<String>(userSet);
    }

    private static List<String> diff( List<String> minuend, List<String> subtrahend ) {
        Set<String> strSet = new TreeSet<>(minuend);
        strSet.removeAll(subtrahend);
        return strSet.size() > 0 ? new ArrayList<>(strSet) : null;
    }

    private static String createLocation(ObjectDesc desc) {
        String random = UUID.randomUUID().toString();
        String[] splits = random.split("-");
        String last = splits[splits.length-1];
        return String.format("%s-%s", desc.objectId, last);
    }

    private static boolean consistent(Object oldVal, Object newVal) {
        return newVal == null || newVal.equals(oldVal);
    }

    private static ErrorDesc notFoundErr(String objectId) {
        return new ErrorDesc(Response.Status.NOT_FOUND,String.format(messagesConf.get("objectNotFound"),objectId));
    }

    private static ErrorDesc goneErr(String objectId) {
        return new ErrorDesc(Response.Status.GONE,String.format(messagesConf.get("objectDeleted"),objectId));
    }

    private static ErrorDesc permsErr(String objectId, String userName, String op) {
        return new ErrorDesc(Response.Status.FORBIDDEN,String.format(messagesConf.get("permission"),op,objectId,userName));
    }

    private static ErrorDesc badReqErr(String message) {
        return new ErrorDesc(Response.Status.BAD_REQUEST,message);
    }

    DBI mDBI;
    private ObjectStore mLocalStore;
    private ObjectStore mCloudStore;
    static private Long gDefaultEstSize = new Long(-1);
    private static  MessageConfiguration messagesConf;
}


