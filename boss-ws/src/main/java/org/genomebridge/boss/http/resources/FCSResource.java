package org.genomebridge.boss.http.resources;

import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Path("fcs/{objName : [^?]+}")
@JsonInclude(Include.NON_NULL)
public class FCSResource {

    @GET
    public Response getData(@PathParam("objName") String objName) {
        byte[] value = mMap.get(objName);
        if ( value == null )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().entity(value).build();
    }

    @HEAD
    public Response checkData(@PathParam("objName") String objName) {
        if ( !mMap.containsKey(objName) )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    // TODO: add POST method for testing multi-part upload

    @PUT
    public Response putData(@PathParam("objName") String objName,
                            @HeaderParam(HttpHeaders.CONTENT_LENGTH) String contentLength,
                            @HeaderParam("x-goog-copy-source") String copySource,
                            byte[] value) {
        // check silly GCS quirk
        if ( contentLength == null )
            return Response.status(Response.Status.BAD_REQUEST).build();

        if ( copySource != null ) {
            if ( value != null && value.length > 0 )
                return Response.status(Response.Status.BAD_REQUEST).build();
            if ( copySource.length() > 0 )
                value = mMap.get(copySource.substring(1));
            if ( value == null )
                return Response.status(Response.Status.NOT_FOUND).build();
        }
        mMap.put(objName, value);
        return Response.ok().build();
    }

    @DELETE
    public Response deleteData(@PathParam("objName") String objName) {
        if ( mMap.remove(objName) == null )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    private ConcurrentHashMap<String,byte[]> mMap = new ConcurrentHashMap<>();
}
