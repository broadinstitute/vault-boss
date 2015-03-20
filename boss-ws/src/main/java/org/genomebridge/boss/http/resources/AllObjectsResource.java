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
package org.genomebridge.boss.http.resources;

import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/objects")
public class AllObjectsResource extends AbstractResource {

    public AllObjectsResource( BossAPI api ) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    public Response findObjectsByName( @QueryParam("name") String objectName,
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
    public Response createObject( @Context UriInfo info,
                                  @HeaderParam(REMOTE_USER_HEADER) String userName,
                                  ObjectDesc req ) {
        ErrorDesc err = api.insertObject(req,userName);
        if ( err != null )
            throwWAE(err);
        URI uri = info.getBaseUriBuilder().path("/objects/{objectId}").build(req.objectId);
        return Response.created(uri).entity(req).build();
    }

    private BossAPI api;
}
