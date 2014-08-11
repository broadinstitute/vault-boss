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

import org.genomebridge.boss.http.objectstore.HttpMethod;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;
import java.util.*;

public class MemoryBossAPI implements BossAPI {

    private Map<String,GroupResource> groups;
    private Map<String,ObjectResource> objects;
    private Set<String> deregisteredObjects;

    public MemoryBossAPI() {
        groups = new TreeMap<>();
        objects = new TreeMap<>();
        deregisteredObjects = new TreeSet<>();
    }

    public String composite(String groupId, String objectId) {
        return String.format("%s/%s", groupId, objectId);
    }

    public String key(ObjectResource rec) { return composite(rec.group, rec.objectId); }

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
    public void updateObject(ObjectResource rec) {
        String id = key(rec);
        objects.put(id, rec);
    }

    @Override
    public URI getPresignedURL(ObjectResource rec, HttpMethod method, long millis) {
        return URI.create(String.format("http://localhost:8080/%s/presigned_url", rec.objectId));
    }
}
