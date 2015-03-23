package org.genomebridge.boss.http.resources;


import com.fasterxml.jackson.annotation.JsonInclude;

import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPI.CopyRequest;
import org.genomebridge.boss.http.service.BossAPI.CopyResponse;
import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;
import org.genomebridge.boss.http.service.BossAPI.ResolveRequest;
import org.genomebridge.boss.http.service.BossAPI.ResolveResponse;

import javax.ws.rs.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@Path("objects/{objectId}")
@JsonInclude(Include.NON_NULL)
public class ObjectResource extends AbstractResource {

    public ObjectResource( BossAPI api ) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    public ObjectDesc describe(@PathParam("objectId") String objectId,
                               @HeaderParam(REMOTE_USER_HEADER) String userName) {
        ObjectDesc desc = new ObjectDesc();
        ErrorDesc err = api.getObject(objectId, userName, desc);
        if ( err != null )
            throwWAE(err);
        return desc;
    }

    @Path("resolve")
    @Produces("application/json")
    @POST
    public ResolveResponse resolve(@PathParam("objectId") String objectId,
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
    public ObjectDesc update(@PathParam("objectId") String objectId,
                             @HeaderParam(REMOTE_USER_HEADER) String userName,
                             ObjectDesc desc) {
        ErrorDesc err = api.updateObject(desc,objectId,userName);
        if ( err != null )
            throwWAE(err);
        return desc;
    }

    @Path("copy")
    @Consumes("application/json")
    @Produces("application/json")
    @POST
    public CopyResponse resolveForCopy(@PathParam("objectId") String objectId,
                                       @HeaderParam("REMOTE_USER") String userName,
                                       CopyRequest req) {
        CopyResponse resp = new CopyResponse();
        ErrorDesc err = api.resolveObjectForCopying(objectId, userName, req, resp);
        if ( err != null )
            throwWAE(err);
        return resp;
    }

    @DELETE
    public String delete(@PathParam("objectId") String objectId,
                         @HeaderParam(REMOTE_USER_HEADER) String userName) {
        ErrorDesc err = api.deleteObject(objectId,userName);
        if ( err != null )
            throwWAE(err);
        return objectId;
    }

    private BossAPI api;
}
