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
import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import java.net.URI;

public interface BossAPI {

    public GroupResource getGroup(String groupId);
    public void updateGroup(GroupResource rec);

    public ObjectResource getObject(String objectId, String groupId);
    public void updateObject(ObjectResource rec);
    public void deregisterObject(ObjectResource rec);

    public FsGroupResource getFsGroup(String groupId);
    public void updateFsGroup(FsGroupResource rec);

    public FsObjectResource getFsObject(String objectId, String groupId);
    public void updateFsObject(FsObjectResource rec);
    public void deregisterFsObject(FsObjectResource rec);

    public URI getPresignedURL(ObjectResource rec, HttpMethod method, long millis);
}
