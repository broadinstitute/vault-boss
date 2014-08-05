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

import org.genomebridge.boss.http.db.BossDAO;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("db")
public class GroupDBResource {

    private BossDAO dao;

    public GroupDBResource() {}

    public GroupDBResource(BossDAO dao) {
       this.dao = dao;
    }

    @Path("group/{id}")
    @Produces("application/json")
    @GET
    public GroupResource group(@PathParam("id") String id) {
        return dao.findGroupById(id);
    }

    @Path("group")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response insert( @Context UriInfo info, GroupResource rec ) {
        dao.insertGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint, null);
        URI uri = info.getRequestUriBuilder().path("{groupId}").build(rec.groupId);
        return Response.created(uri).entity(rec).build();
    }
}
