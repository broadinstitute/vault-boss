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
import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;
import java.util.*;

public class DatabaseBossAPI implements BossAPI {

    private BossDAO dao;

    private ObjectStore objectStore;

    @Inject
    public DatabaseBossAPI( BossDAO dao, ObjectStore store ) {
        this.dao = dao;
        this.objectStore = store;
    }

    private String composite(String groupId, String objectId) {
        return String.format("%s/%s", groupId, objectId);
    }

    private String id(GroupResource rec) {
        return rec.groupId;
    }

    private String id(FsGroupResource rec) {
        return "fs-" + rec.groupId;
    }

    private String id(ObjectResource rec) {
        return composite(rec.group, rec.objectId);
    }

    private String id(FsObjectResource rec) {
        return composite("fs-" + rec.group, rec.objectId);
    }

    public static String location(ObjectResource rec) {
        String random = UUID.randomUUID().toString();
        String[] splits = random.split("-");
        String last = splits[splits.length-1];
        return String.format("%s/%s-%s", rec.group, rec.objectId, last);
    }

    private void updateReaders(String id, String[] target) {
        updateReaders(id, target != null ? Arrays.asList(target) : new ArrayList<String>());
    }

    private void updateWriters(String id, String[] target) {
        updateWriters(id, target != null ? Arrays.asList(target) : new ArrayList<String>());
    }

    private void updateReaders(String id, Collection<String> target) {
        Set<String> current = new TreeSet<>(dao.findReadersById(id));
        Set<String> toDelete = new TreeSet<>(current);
        Set<String> toAdd = new TreeSet<>();
        for(String user : target) {
            toDelete.remove(user);
            if(!current.contains(user)) { toAdd.add(user); }
        }

        dao.deleteReaders(id, new ArrayList<>(toDelete));
        dao.insertReaders(id, new ArrayList<>(toAdd));
    }
    private void updateWriters(String id, Collection<String> target) {
        Set<String> current = new TreeSet<>(dao.findWritersById(id));
        Set<String> toDelete = new TreeSet<>(current);
        Set<String> toAdd = new TreeSet<>();
        for(String user : target) {
            toDelete.remove(user);
            if(!current.contains(user)) { toAdd.add(user); }
        }

        dao.deleteWriters(id, new ArrayList<>(toDelete));
        dao.insertWriters(id, new ArrayList<>(toAdd));
    }

    @Override
    public GroupResource getGroup(String groupId) {
        GroupResource rec = dao.findGroupById(groupId);
        if(rec != null) {
            rec.readers = dao.findReadersById(groupId).toArray(new String[0]);
            rec.writers = dao.findWritersById(groupId).toArray(new String[0]);
        }
        return rec;
    }

    @Override
    public void updateGroup(GroupResource rec) {
        assert(rec.groupId != null);

        if(dao.findGroupById(rec.groupId) != null) {
            dao.updateGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint);
        } else {
            dao.insertGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint, null);
        }
        updateReaders(rec.groupId, rec.readers);
        updateWriters(rec.groupId, rec.writers);
    }

    @Override
    public ObjectResource getObject(String objectId, String groupId) {
        ObjectResource rec = dao.findObjectById(objectId, groupId);
        if(rec != null) {
            rec.readers = dao.findReadersById(id(rec)).toArray(new String[0]);
            rec.writers = dao.findWritersById(id(rec)).toArray(new String[0]);
        }
        return rec;
    }

    @Override
    public void updateObject(ObjectResource rec) {
        if(dao.findObjectById(rec.objectId, rec.group) != null) {
            dao.updateObject(rec.objectId, rec.group, rec.ownerId, rec.sizeEstimateBytes, rec.name);
        } else {
            dao.insertObject(rec.objectId, rec.group, rec.ownerId, rec.sizeEstimateBytes, rec.name, location(rec));
        }
        String id = id(rec);
        updateReaders(id, rec.readers);
        updateWriters(id, rec.writers);
    }

    @Override
    public void deregisterObject(ObjectResource rec) {
        dao.deleteObject(rec.objectId, rec.group);
    }

    @Override
    public FsGroupResource getFsGroup(String groupId) {
        String id = "fs-" + groupId;
        FsGroupResource rec = dao.findFsGroupById(id);
        if(rec != null) {
            rec.readers = dao.findReadersById(id).toArray(new String[0]);
            rec.writers = dao.findWritersById(id).toArray(new String[0]);
        }
        return rec;
    }

    @Override
    public void updateFsGroup(FsGroupResource rec) {
        String id = id(rec);
        if(dao.findFsGroupById(id) != null) {
            dao.updateGroup(id, rec.ownerId, -1L, rec.typeHint);
        } else {
            dao.insertGroup(id, rec.ownerId, -1L, rec.typeHint, rec.directory);
        }
        updateReaders(id, rec.readers);
        updateWriters(id, rec.writers);
    }

    @Override
    public FsObjectResource getFsObject(String objectId, String groupId) {
        String gid = "fs-" + groupId;
        String id = composite(groupId, objectId);
        FsObjectResource rec = dao.findFsObjectById(id, gid);
        if(rec != null) {
            rec.readers = dao.findReadersById(id).toArray(new String[0]);
            rec.writers = dao.findWritersById(id).toArray(new String[0]);
        }
        return rec;
    }

    @Override
    public void updateFsObject(FsObjectResource rec) {
        String gid = "fs-" + rec.group;
        String oid = composite(rec.group, rec.objectId);
        if(dao.findFsObjectById(oid, gid) != null) {
            dao.updateObject(oid, gid, rec.ownerId, rec.sizeEstimateBytes, rec.name);
        } else {
            dao.insertObject(oid, gid, rec.ownerId, rec.sizeEstimateBytes, rec.name, null);
        }
        updateReaders(oid, rec.readers);
        updateWriters(oid, rec.writers);
    }

    @Override
    public void deregisterFsObject(FsObjectResource rec) {
        String gid = "fs-" + rec.group;
        String oid = composite(rec.group, rec.objectId);
        dao.deleteObject(oid, gid);
    }

    @Override
    public URI getPresignedURL(ObjectResource rec, HttpMethod method, long millis) {
        String location = dao.findObjectLocation(rec.objectId, rec.group);
        return objectStore.generatePresignedURL(location, method, millis);
    }
}
