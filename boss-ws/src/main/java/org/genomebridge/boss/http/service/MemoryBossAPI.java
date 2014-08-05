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

import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;
import java.util.*;

public class MemoryBossAPI implements BossAPI {

    private Map<String,GroupResource> groups;
    private Map<String,ObjectResource> objects;
    private Map<String,FsGroupResource> fsgroups;
    private Map<String,FsObjectResource> fsobjects;
    private Set<String> deregisteredObjects;

    public MemoryBossAPI() {
        groups = new TreeMap<>();
        objects = new TreeMap<>();
        fsgroups = new TreeMap<>();
        fsobjects = new TreeMap<>();
        deregisteredObjects = new TreeSet<>();
    }

    public String composite(String groupId, String objectId) {
        return String.format("%s/%s", groupId, objectId);
    }

    public String key(ObjectResource rec) { return composite(rec.group, rec.objectId); }
    public String key(FsObjectResource rec) { return composite(rec.group, rec.objectId); }

    @Override
    public GroupResource getGroup(String groupId) {
        return groups.get(groupId);
    }

    @Override
    public void updateGroup(GroupResource rec) {
        groups.put(rec.groupId, rec);
    }

    @Override
    public ObjectResource getObject(String objectId, String groupId) {
        String id = composite(groupId, objectId);
        if(deregisteredObjects.contains(id)) {
            throw new DeregisteredObjectException(id);
        }
        return objects.get(id);
    }

    @Override
    public void deregisterObject(ObjectResource object) {
        String id = key(object);
        if(objects.containsKey(id)) {
            objects.remove(id);
            deregisteredObjects.add(id);
        } else {
            throw new DeregisteredObjectException(id);
        }
    }

    @Override
    public FsGroupResource getFsGroup(String groupId) { return fsgroups.get(groupId); }

    @Override
    public void updateFsGroup(FsGroupResource rec) { fsgroups.put(rec.groupId, rec); }

    @Override
    public FsObjectResource getFsObject(String objectId, String groupId) {
        String id = composite(groupId, objectId);
        if(deregisteredObjects.contains(id)) {
            throw new DeregisteredObjectException(id);
        }
        return fsobjects.get(id);
    }

    @Override
    public void updateFsObject(FsObjectResource rec) {
        String id = key(rec);
        fsobjects.put(id, rec);
    }

    @Override
    public void deregisterFsObject(FsObjectResource rec) {
        String id = key(rec);
        if(fsobjects.containsKey(id)) {
            fsobjects.remove(id);
            deregisteredObjects.add(id);
        } else {
            throw new DeregisteredObjectException(id);
        }
    }

    @Override
    public void updateObject(ObjectResource rec) {
        String id = key(rec);
        objects.put(id, rec);
    }

    @Override
    public URI getPresignedURL(int seconds) {
        return URI.create("http://localhost:8080/presigned_url");
    }
}
