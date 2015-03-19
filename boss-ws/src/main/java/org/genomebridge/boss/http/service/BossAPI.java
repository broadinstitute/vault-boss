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

import org.genomebridge.boss.http.models.ObjectCore;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.Response;

public interface BossAPI {

    public static class ObjectDesc extends ObjectCore {
        public String[] readers, writers;
    }

    public static class ErrorDesc {
        public ErrorDesc( Response.Status status, String message ) {
            mStatus = status;
            mMessage = message;
        }
        public Response.Status mStatus;
        public String mMessage;
    }

    public ErrorDesc getObject(String objectId, String userName, ObjectDesc desc);
    public ErrorDesc findObjectsByName(String objectName, String userName, List<ObjectDesc> descs);
    public ErrorDesc insertObject(ObjectDesc desc, String userName);
    public ErrorDesc updateObject(ObjectDesc desc, String objectId, String userName);
    public ErrorDesc deleteObject(String objectId, String userName);

    public static class ResolveRequest {
        public Integer validityPeriodSeconds;
        public String httpMethod;
        public String contentType;
        public String contentMD5Hex;
    }

    public static class ResolveResponse {
        public URI objectUrl;
        public Integer validityPeriodSeconds;
        public String contentType;
        public String contentMD5Hex;
    }

    public ErrorDesc resolveObject(String objectId, String userName, ResolveRequest req, ResolveResponse resp);

    public static class CopyRequest {
        public Integer validityPeriodSeconds;
        public String locationToCopy; // expecting something of the form "/bucket/key"
    }

    public static class CopyResponse {
        public URI uri;
    }

    public ErrorDesc resolveObjectForCopying(String objectId, String userName, CopyRequest req, CopyResponse resp);
}
