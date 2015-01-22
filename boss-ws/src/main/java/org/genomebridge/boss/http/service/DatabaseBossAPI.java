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
import org.genomebridge.boss.http.objectstore.ObjectStoreException;
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
    public void updateObject(String objectId, ObjectResource rec) {
        if(dao.findObjectById(objectId) != null) {
            dao.updateObject(objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, rec.storagePlatform);
        } else {

            /*
            Use the location passed in by the user if the Object is a 'filesystem'
            type object, otherwise generate a new (fresh) location.
             */
            String loc = rec.storagePlatform.equals("filesystem") ?
                    rec.directoryPath : location(rec);

            dao.insertObject(objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, loc, rec.storagePlatform);
        }
        updateReaders(objectId, rec.readers);
        updateWriters(objectId, rec.writers);
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
        dao.begin();
        dao.updateObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, now);
        dao.insertReaders(rec.objectId, readersToInsert);
        dao.deleteReaders(rec.objectId, readersToDelete);
        dao.insertWriters(rec.objectId, writersToInsert);
        dao.deleteWriters(rec.objectId, writersToDelete);
        dao.commit();
    }

    @Override
    public void deleteObject(ObjectResource rec) {
       /* if this object resides in the object store, also delete from the object store.

           BOSS rev3 spec says: If BOSS is unable to delete the underlying object from the object storage
           (e.g. non-transient network failure, or other error from object store server), it will return an
           appropriate 50x error, and the entry for this object will not be deleted from BOSS.

           So, we delete from the db first, then the object store. If object store deletion fails,
           we'll trigger the try/catch and rollback the db deletion.
        */
        Boolean isObjectStore = rec.isObjectStoreObject();
        String location = dao.findObjectLocation(rec.objectId);

        dao.begin();

        // Try to remove object resource first so we don't end up with orphaned records.
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            dao.deleteObject(rec.objectId, now);
        } catch (Exception e) {
            dao.rollback();
            throw new ObjectStoreException("Unable to delete object resource.", e);
        }

        // Only commit the ObjectResource delete after checking for ObjectStore deletion.
        try {
            if (isObjectStore && location != null) {
                objectStore.deleteObject(location);
            }
            dao.commit();
        } catch (Exception e) {
            dao.rollback();
            throw new ObjectStoreException("Unable to delete object from object store.", e);
        }
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
