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

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPIProvider;

import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.UUID;
import java.util.List;

@Path("/objects")
public class AllObjectsResource {

    private BossAPI api;

    public AllObjectsResource() {
        this.api = BossAPIProvider.getInstance().getApi();
    }

    public String randomID() { return UUID.randomUUID().toString(); }

    @GET
    @Produces("application/json")
    public Response findObjectsByName( @QueryParam("name") String objectName,
                                       @Context HttpHeaders headers ) {
        String username = headers.getRequestHeaders().getFirst("REMOTE_USER");
        List<ObjectResource> recs = api.findObjectsByName(username, objectName);
        if ( recs.size() == 0 )
            throw new NotFoundException("There are no objects by the name: " + objectName);
        for ( ObjectResource rec : recs )
            if (rec.storagePlatform.equals(StoragePlatform.OBJECTSTORE.getValue()))
                rec.directoryPath = null;
        return Response.status(Response.Status.OK).type("application/json").entity(recs).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createObject( @Context UriInfo info, @Context HttpHeaders headers, ObjectResource rec ) {
        String errMsg = rec.testValidity();
        if ( errMsg != null )
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(errMsg).build());

        rec.objectId = randomID();
        api.insertObject(rec,headers.getRequestHeaders().getFirst("REMOTE_USER"));

        URI uri = info.getBaseUriBuilder().path("/objects/{objectId}").build(rec.objectId);
        return Response.status(Response.Status.CREATED)
                .location(uri)
                .type("application/json")
                .entity(rec)
                .build();
    }
}
