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

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("group/{groupId}")
public class GroupResource extends AbstractResource {

    private BossAPI api;

    @PathParam("groupId") public String groupId;

    public String ownerId;
    public Long sizeEstimateBytes;
    public String typeHint;
    public String[] readers, writers;

    public GroupResource() {}

    @Inject
    public GroupResource(BossAPI api) {
        this.api = api;
    }

    @Path("{objectId}")
    public ObjectResource getObject(@Context UriInfo uriInfo,
                                    @PathParam("objectId") String objectId) {
        return new ObjectResource(api, uriInfo.getRequestUri().toString());
    }

    @GET
    @Produces("application/json")
    public GroupResource describe() {

        populateFromAPI();

        return this;
    }

    private void populateFromAPI() {
        GroupResource rec = api.getGroup(groupId);

        ownerId = rec.ownerId;
        sizeEstimateBytes = rec.sizeEstimateBytes;
        typeHint = rec.typeHint;
        readers = rec.readers;
        writers = rec.writers;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public GroupResource update(GroupResource newrec) {
        if(newrec.groupId != null) {
            throw new IllegalArgumentException("non-null groupId update isn't allowed");
        }

        this.ownerId = setFrom(ownerId, newrec.ownerId);
        this.sizeEstimateBytes = setFrom(sizeEstimateBytes, newrec.sizeEstimateBytes);
        this.typeHint = setFrom(typeHint, newrec.typeHint);
        this.readers = setFrom(readers, newrec.readers);
        this.writers = setFrom(writers, newrec.writers);

        updateInAPI();

        return this;
    }

    private void updateInAPI() {
        api.updateGroup(this);
    }

    public boolean equals(Object o) {
        if(!(o instanceof GroupResource)) { return false; }
        GroupResource r = (GroupResource)o;

        return groupId.equals(r.groupId) &&
                eq(ownerId, r.ownerId) &&
                eq(sizeEstimateBytes, r.sizeEstimateBytes) &&
                eq(typeHint, r.typeHint) &&
                arrayEq(readers, r.readers) &&
                arrayEq(writers, r.writers);
    }

    public int hashCode() {
        int code = 17;
        code += groupId.hashCode(); code *= 37;
        if(ownerId != null) { code += ownerId.hashCode(); code *= 37; }
        return code;
    }
}
