package org.genomebridge.boss.http.resources;

import com.wordnik.swagger.annotations.*;
import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;


import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/objects")
@Api(value = "objects", description = "Operations on an object resource", produces = "application/json")
public class AllObjectsResource extends AbstractResource {

    public AllObjectsResource( BossAPI api ) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Querying for Objects by Name",
                  notes = "Objects can be queried by GETting that name from the /objects URL with the parameter name=objectName.",
                  response = ObjectDesc.class,
                  responseContainer = "List",
                  httpMethod = "GET")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful Request"),
            @ApiResponse(code = 404, message = "Object Name Not Found"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
           }
    )
    public Response findObjectsByName(@ApiParam(name = "name",  required = true, value = "Object Name")
                                      @QueryParam("name")String objectName,
                                      @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                                      @HeaderParam(REMOTE_USER_HEADER) String userName ) {
        List<ObjectDesc> recs = new ArrayList<>();
        ErrorDesc err = api.findObjectsByName(objectName, userName, recs);
        if ( err != null )
            throwWAE(err);
        return Response.ok(recs).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(value = "Creating Objects",
                  notes = "Objects can be created by POSTing a JSON document to the /objects URL.",
                  response = ObjectDesc.class,
                  httpMethod = "POST"
                 )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful Operation"),
            @ApiResponse(code = 400, message = "Malformed Input"),
            @ApiResponse(code = 500, message = "Boss Internal Error")
        }
    )
    public Response createObject( @Context UriInfo info,
                                  @ApiParam(name = REMOTE_USER_HEADER,  required = true, value = "Remote User")
                                  @HeaderParam(REMOTE_USER_HEADER) String userName,
                                  @ApiParam(required = true, value = "A JSON representation of an Object, except for the 'objectId' field (which is generated)")
                                  ObjectDesc req ) {
        ErrorDesc err = api.insertObject(req,userName);
        if ( err != null )
            throwWAE(err);
        URI uri = info.getBaseUriBuilder().path("/objects/{objectId}").build(req.objectId);
        return Response.created(uri).entity(req).build();
    }

    private BossAPI api;
}
