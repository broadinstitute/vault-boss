package org.genomebridge.boss.http.resources;


import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;


import org.genomebridge.boss.http.models.CopyRequest;
import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.models.ResolveRequest;
import org.genomebridge.boss.http.models.ResolveResponse;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPI.CopyResponse;
import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("objects/{objectId}")
@JsonInclude(Include.NON_NULL)
@Api(value = "objects")
public class ObjectResource extends AbstractResource {

    public ObjectResource( BossAPI api ) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Describing Objects",
                  notes = "Objects can be queried by GETting from the /objects URL with the parameter objectId.",
                  response = ObjectDesc.class,
                  httpMethod = "GET")

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Request"),
            @ApiResponse(code = 403, message = "Required Permissions Not Given"),
            @ApiResponse(code = 404, message = "Object Id Not Found"),
            @ApiResponse(code = 410, message = "Object Has Been Deleted"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public ObjectDesc describe(@ApiParam(name =  "objectId",  required = true, value = "Object Boss Id")
                               @PathParam("objectId") String objectId,
                               @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                               @HeaderParam(REMOTE_USER_HEADER) String userName) {
        ObjectDesc desc = new ObjectDesc();
        ErrorDesc err = api.getObject(objectId, userName, desc);
        if ( err != null )
            throwWAE(err);
        return desc;
    }

    @Path("/resolve")
    @Produces("application/json")
    @POST
    @ApiOperation(value = "Resolving Objects",
                  notes = "Objects are 'resolved' into an URL which can be used to upload or download the resource. For an 'opaqueURI' object, it will return the URI passed when the object was created. In a 'localStore' or 'cloudStore' object, resolve will return a pre-signed URL (for uploading or downloading according to the operation). If it's not specified, the default operation is download.",
                  response = ResolveResponse.class,
                  httpMethod = "POST")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 403, message = "Required Permissions Not Given"),
            @ApiResponse(code = 404, message = "Object Id Not Found"),
            @ApiResponse(code = 410, message = "Object Has Been Deleted"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public ResolveResponse resolve(@ApiParam(required = true,value = "Object Boss Id")
                                   @PathParam("objectId") String objectId,
                                   @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                                   @HeaderParam(REMOTE_USER_HEADER) String userName,
                                   ResolveRequest req) {
        ResolveResponse resp = new ResolveResponse();
        ErrorDesc err = api.resolveObject(objectId, userName, req, resp);
        if ( err != null )
            throwWAE(err);
        return resp;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(value = "Updating Objects",
                  notes = "Object resources can be updated by POSTing a modified representation of the resource to the URL. Only the ownerId, readers, and writers fields can be modified through POSTs to an Object resource.",
                  response = ObjectDesc.class,
                  httpMethod = "POST"
                 )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 400, message = "Malformed Input"),
            @ApiResponse(code = 403, message = "Required Permissions Not Given"),
            @ApiResponse(code = 404, message = "Object Id Not Found"),
            @ApiResponse(code = 410, message = "Object Has Been Deleted"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
       }
    )
    public ObjectDesc update(@ApiParam(required = true,value = "Object Boss Id")
                             @PathParam("objectId") String objectId,
                             @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                             @HeaderParam(REMOTE_USER_HEADER) String userName,
                             ObjectDesc desc) {

        ErrorDesc err = api.updateObject(desc,objectId,userName);
        if (err != null )
            throwWAE(err);
        return desc;
    }

    @Path("/copy")
    @Consumes("application/json")
    @Produces("application/json")
    @POST
    @ApiOperation(value = "Copying Objects",
                  notes = "The Copy service returns an URL which can write data to a BOSS Object. But the ultimate source of the data is some other object in the same object store. Currently this works only for cloudStore objects stored in GCS.",
                  httpMethod = "POST",
                  response = CopyResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 403, message = "Required Permissions Not Given"),
            @ApiResponse(code = 404, message = "Object Id Not Found"),
            @ApiResponse(code = 410, message = "Object Has Been Deleted"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
         }
    )
    public CopyResponse resolveForCopy(@ApiParam(required = true,value = "Object Boss Id")
                                       @PathParam("objectId") String objectId,
                                       @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                                       @HeaderParam("REMOTE_USER") String userName,
                                       CopyRequest req) {
        CopyResponse resp = new CopyResponse();
        ErrorDesc err = api.resolveObjectForCopying(objectId, userName, req, resp);
        if (err != null )
            throwWAE(err);
        return resp;
    }

    @DELETE
    @ApiOperation(value = "Deleting Objects",
                  notes = "For an object in the 'localStore' or 'cloudStore' platform, it will also delete the underlying object. For an 'opaqueURI' object, deleting whatever the URI references is the caller’s responsibility",
                  httpMethod = "DELETE")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 403, message = "Required Permissions Not Given"),
            @ApiResponse(code = 404, message = "Object Id Not Found"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public String delete(@ApiParam(required = true,value = "Object Boss Id")
                         @PathParam("objectId") String objectId,
                         @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                         @HeaderParam(REMOTE_USER_HEADER) String userName) {
        ErrorDesc err = api.deleteObject(objectId,userName);
        if (err != null )
            throwWAE(err);
        return objectId;
    }

    @Path("multi")
    @Produces("application/json")
    @POST
    public CopyResponse getUploadID(@PathParam("objectId") String objectId,
                                    @HeaderParam(REMOTE_USER_HEADER) String userName) {
        CopyResponse resp = new CopyResponse();
        ErrorDesc err = api.getResumableUploadURL(objectId, userName, resp);
        if ( err != null ) {
            throwWAE(err);
        }
        return resp;
    }

    private BossAPI api;
}
