package org.genomebridge.boss.http.resources;

import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.wordnik.swagger.annotations.*;

@Path("fcs/{objName : [^?]+}")
@JsonInclude(Include.NON_NULL)
@Api(value = "fcs", description = "Operations on object's data",produces = "application/json")
public class FCSResource {

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Querying for Object's data by objName",
                  httpMethod = "GET",
            response = Byte.class,
            responseContainer = "Array")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Request"),
            @ApiResponse(code = 404, message = "Object Name Not Found"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public Response getData(@ApiParam(name =  "objName",  required = true, value = "Object Name")
                            @PathParam("objName") String objName) {
        byte[] value = mMap.get(objName);
        if ( value == null )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().entity(value).build();
    }

    @HEAD
    @Produces("application/json")
    @ApiOperation(value = "Checking for Object's data by objName",
                  httpMethod = "HEAD")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 404, message = "Object Name Not Found"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public Response checkData(@ApiParam(name =  "objName",  required = true, value = "Object Name")
                              @PathParam("objName") String objName) {
        if ( !mMap.containsKey(objName) )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    // TODO: add POST method for testing multi-part upload

    @PUT
    @Produces("application/json")
    @ApiOperation(value = "Inserting Object's data by objName",
                  httpMethod = "PUT")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 404, message = "Object Name Not Found"),
            @ApiResponse(code = 400, message = "Malformed Input"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public Response putData(@ApiParam(name =  "objName",  required = true, value = "Object Name")
                            @PathParam("objName") String objName,
                            @ApiParam(name = "Content-Length",  required = true, value = "Content Length")
                            @HeaderParam(HttpHeaders.CONTENT_LENGTH) String contentLength,
                            @ApiParam(name = "x-goog-copy-source",  required = true, value = "Location to copy")
                            @HeaderParam("x-goog-copy-source") String copySource,
                            @ApiParam(name="value",required = true) byte[] value) {
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
    @Produces("application/json")
    @ApiOperation(value = "Deleting Object's data by objName",
                  httpMethod = "DELETE")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Operation"),
            @ApiResponse(code = 404, message = "Object Name Not Found"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public Response deleteData(@ApiParam(name = "objName",  required = true, value = "Object Name")
                               @PathParam("objName") String objName) {
        if ( mMap.remove(objName) == null )
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    private ConcurrentHashMap<String,byte[]> mMap = new ConcurrentHashMap<>();
}
