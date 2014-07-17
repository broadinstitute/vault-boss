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

import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;
import java.util.*;

public class MemoryBossAPI implements BossAPI {

    private Map<String,GroupResource> groups;
    private Map<String,ObjectResource> objects;
    private Set<String> deregisteredObjects;

    public MemoryBossAPI() {
        groups = new TreeMap<String,GroupResource>();
        objects = new TreeMap<String,ObjectResource>();
        deregisteredObjects = new TreeSet<String>();
    }

    @Override
    public GroupResource getGroup(String groupId) {
        return groups.get(groupId);
    }

    @Override
    public void updateGroup(GroupResource rec) {
        groups.put(rec.groupId, rec);
    }

    @Override
    public ObjectResource getObject(String objectId) {
        if(deregisteredObjects.contains(objectId)) {
            throw new DeregisteredObjectException(objectId);
        }
        return objects.get(objectId);
    }

    @Override
    public void deregisterObject(ObjectResource object) {
        if(objects.containsKey(object.objectId)) {
            objects.remove(object.objectId);
            deregisteredObjects.add(object.objectId);
        } else {
            throw new DeregisteredObjectException(object.objectId);
        }
    }

    @Override
    public void updateObject(ObjectResource rec) {
        objects.put(rec.objectId, rec);
    }

    @Override
    public URI getPresignedURL(int seconds) {
        return URI.create("http://localhost:8080/presigned_url");
    }
}
