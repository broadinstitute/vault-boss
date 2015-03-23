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
