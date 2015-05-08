package org.genomebridge.boss.http.service;


import java.net.URI;
import java.util.List;

import javax.ws.rs.core.Response;

import org.genomebridge.boss.http.models.CopyRequest;
import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.models.ResolveRequest;
import org.genomebridge.boss.http.models.ResolveResponse;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

public interface BossAPI {




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
    public ErrorDesc resolveObject(String objectId, String userName, ResolveRequest req, ResolveResponse resp);
    public ErrorDesc getResumableUploadURL(String objectId, String userName, CopyResponse resp);


    @ApiModel("Copy Response")
    public static class CopyResponse {
        @ApiModelProperty(value="A pre-signed url generated to access the object for the desired operation.")
        public URI uri;
    }

    public ErrorDesc resolveObjectForCopying(String objectId, String userName, CopyRequest req, CopyResponse resp);
}
