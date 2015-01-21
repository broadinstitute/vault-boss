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

import com.google.inject.Inject;

import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.HttpMethod;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

/**
 * Basically, a shim between the BossAPI and the BossDAO (which is defined as a JDBI-style annotated
 * interface, but is the class which handles direct interaction with the database).
 */
public class DatabaseBossAPI implements BossAPI {

    private BossDAO dao;

    private ObjectStore objectStore;
    static private Long gDefaultEstSize = new Long(-1);

    @Inject
    public DatabaseBossAPI( BossDAO dao, ObjectStore store ) {
        this.dao = dao;
        this.objectStore = store;
    }

    public static String location(ObjectResource rec) {
        String random = UUID.randomUUID().toString();
        String[] splits = random.split("-");
        String last = splits[splits.length-1];
        return String.format("%s-%s", rec.objectId, last);
    }

    @Override
    public ObjectResource getObject(String objectId) {
        ObjectResource rec = dao.findObjectById(objectId);
        if ( rec != null ) {
            if ( !rec.active.equals("Y") )
                return null;
            rec.readers = dao.findReadersById(objectId).toArray(new String[0]);
            rec.writers = dao.findWritersById(objectId).toArray(new String[0]);
        }
        return rec;
    }

    @Override
    public List<ObjectResource> findObjectsByName(String username, String objectName) {
        List<ObjectResource> recs = dao.findObjectsByName(username, objectName);
        String[] emptyArr = new String[0];
        for ( ObjectResource rec : recs ) {
            rec.readers = dao.findReadersById(rec.objectId).toArray(emptyArr);
            rec.writers = dao.findWritersById(rec.objectId).toArray(emptyArr);
        }
        return recs;
    }

    private List<String> uniqueUsers( String[] users ) {
        Set<String> userSet = new TreeSet<>(Arrays.asList(users));
        return new ArrayList<String>(userSet);
    }

    @Override
    public void insertObject(ObjectResource rec, String user) {
        /*
        Use the location passed in by the user if the Object is a 'filesystem'
        type object, otherwise generate a new (fresh) location.
         */
        String loc = rec.storagePlatform.equals("filesystem") ?
                rec.directoryPath : location(rec);

        Long estBytes = rec.sizeEstimateBytes;
        if ( estBytes == null )
            estBytes = gDefaultEstSize;

        List<String> readers = uniqueUsers(rec.readers);
        List<String> writers = uniqueUsers(rec.writers);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        // these next 3 lines need to be in a transaction
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, estBytes, loc, rec.storagePlatform, user, now);
        dao.insertReaders(rec.objectId, readers);
        dao.insertWriters(rec.objectId, writers);
    }

    private List<String> diff( List<String> minuend, List<String> subtrahend ) {
        Set<String> strSet = new TreeSet<>(minuend);
        strSet.removeAll(subtrahend);
        return new ArrayList<>(strSet);
    }

    @Override
    public void updateObject(ObjectResource rec) {
        List<String> newUsers = Arrays.asList(rec.readers);
        List<String> curUsers = dao.findReadersById(rec.objectId);
        List<String> readersToInsert = diff(newUsers,curUsers);
        List<String> readersToDelete = diff(curUsers,newUsers);
        newUsers = Arrays.asList(rec.writers);
        curUsers = dao.findWritersById(rec.objectId);
        List<String> writersToInsert = diff(newUsers,curUsers);
        List<String> writersToDelete = diff(curUsers,newUsers);
        newUsers = null;
        curUsers = null;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // these next 5 lines need to be in a transaction
        dao.updateObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, now);
        dao.insertReaders(rec.objectId, readersToInsert);
        dao.deleteReaders(rec.objectId, readersToDelete);
        dao.insertWriters(rec.objectId, writersToInsert);
        dao.deleteWriters(rec.objectId, writersToDelete);
    }

    @Override
    public void deleteObject(ObjectResource rec) {
        /* if this object resides in the object store, also delete from the object store.

           BOSS rev3 spec says: If BOSS is unable to delete the underlying object from the object storage
           (e.g. non-transient network failure, or other error from objectstore server), it will return an
           appropriate 50x error, and the entry for this object will not be deleted from BOSS.

           So, we delete from object store first, before deleting from the db. If the object store deletion fails,
           we'll trigger the try/catch and won't delete from the db.
        */
        if (rec.isObjectStoreObject()) {
            String location = dao.findObjectLocation(rec.objectId);
            objectStore.deleteObject(location);
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.deleteObject(rec.objectId,now);
    }

    @Override
    public URI getPresignedURL(String objectId, HttpMethod method, long millis, String contentType, byte[] contentMD5) {
        String location = dao.findObjectLocation(objectId);
        if ( location != null ) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            dao.updateResolveDate(objectId,now);
        }
        return objectStore.generatePresignedURL(location, method, millis, contentType, contentMD5);
    }
}
