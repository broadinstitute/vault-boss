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

    @Context UriInfo uriInfo;

    public String groupId;
    public String ownerId;
    public Long sizeEstimateBytes;
    public String typeHint;
    public String[] readers, writers;

    public GroupResource() {}

    @Inject
    public GroupResource(BossAPI api) {
        this.api = api;
    }

    /**
     * Objects are sub-resources of the groups to which they belong.  This method
     * returns an ObjectResource corresponding to the associated objectId within
     * this group.
     *
     * @param objectId The id of the object requested.
     * @return A resource value representing the object requested.
     */
    @Path("{objectId}")
    public ObjectResource getObject(@PathParam("objectId") String objectId) {
        return new ObjectResource(api, uriInfo.getRequestUri().toString());
    }

    /**
     * Returns a populated resource object describing this group resource.
     *
     * This method implements the 'describe' method, for Groups, from the Boss API spec.
     *
     * @return A fully-populated GroupResource containing all available information about this group
     */
    @GET
    @Produces("application/json")
    public GroupResource describe() {

        populateFromAPI();

        return this;
    }

    private boolean populateFromAPI() {

        groupId = uriInfo.getRequestUri().toString();
        GroupResource rec = api.getGroup(groupId);

        if(rec != null) {
            ownerId = rec.ownerId;
            sizeEstimateBytes = rec.sizeEstimateBytes;
            typeHint = rec.typeHint;
            readers = rec.readers;
            writers = rec.writers;

            return true;
        }

        return false;
    }

    /**
     * Updates the resource to match the given template resource.
     *
     * Updates are _only_ accepted on the ownerId, readers, and writers fields; all other
     * updates cause a BAD_REQUEST exception to be thrown.
     *
     * This method implements both the 'reassignOwnership' and 'setPermissions' methods
     * in the Boss API spec.
     *
     * @param newrec The template resource; the ownerId, readers, and writers fields of _this_
     *               resource are updated to match those of the template resource.
     * @return The (updated) resource.
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public GroupResource update(GroupResource newrec) {

        if(populateFromAPI()) {

            this.groupId = errorIfSet(groupId, newrec.groupId);
            this.ownerId = setFrom(ownerId, newrec.ownerId);
            this.sizeEstimateBytes = errorIfSet(sizeEstimateBytes, newrec.sizeEstimateBytes);
            this.typeHint = errorIfSet(typeHint, newrec.typeHint);
            this.readers = setFrom(readers, newrec.readers);
            this.writers = setFrom(writers, newrec.writers);

            updateInAPI();

            return this;
        } else {

            newrec.groupId = uriInfo.getRequestUri().toString();
            api.updateGroup(newrec);
            return newrec;
        }
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
