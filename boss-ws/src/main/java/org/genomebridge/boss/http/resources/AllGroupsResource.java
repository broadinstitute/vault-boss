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

import com.google.inject.Inject;
import org.genomebridge.boss.http.service.BossAPI;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

@Path("/groups")
public class AllGroupsResource {

    private BossAPI api;

    public AllGroupsResource() {}

    @Inject
    public AllGroupsResource(BossAPI api) {
        this.api = api;
    }

    public String randomID() { return UUID.randomUUID().toString(); }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createNewGroup( @Context UriInfo info, GroupResource rec ) {
        rec.groupId = randomID();
        api.updateGroup(rec);

        URI uri = info.getBaseUriBuilder().path("/group/{groupId}").build(rec.groupId);
        return Response.status(Response.Status.CREATED)
                .location(uri)
                .type("application/json")
                .entity(rec)
                .build();
    }
}
