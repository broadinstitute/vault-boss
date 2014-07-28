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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.log4j.Logger;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DeregisteredObjectException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URL;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectResource extends PermissionedResource {

    private BossAPI api;

    public String objectId;
    public String group;

    public Long sizeEstimateBytes;
    public String name;
    public String ownerId;
    public String[] readers, writers;

    public ObjectResource() {
    }

    public ObjectResource(BossAPI api, String groupId, String objectId) {
        this.api = api;
        this.group = groupId;
        this.objectId = objectId;
    }

    public Logger logger() { return Logger.getLogger(String.format("%s/%s", group, objectId)); }
    public void checkUserRead( String user ) { checkUser(user, "READ", readers); }
    public void checkUserWrite( String user ) { checkUser(user, "WRITE", writers); }

    @DELETE
    public String delete(@Context HttpHeaders headers) {
        try {
            populateFromAPI();
            checkUserWrite(headers);
            deleteFromAPI();
            return this.objectId;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private void deleteFromAPI() {
        api.deregisterObject(this);
    }

    @Produces("application/json")
    @GET
    public ObjectResource describe(@Context HttpHeaders headers) {
        try {
            if(!populateFromAPI()) { throw new WebApplicationException(Response.Status.NOT_FOUND); }
            checkUserRead(headers);
            return this;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.GONE);
        }
    }

    private boolean populateFromAPI() {
        ObjectResource fromApi = api.getObject(objectId, group);

        if(fromApi != null) {
            sizeEstimateBytes = fromApi.sizeEstimateBytes;
            name = fromApi.name;
            ownerId = fromApi.ownerId;
            readers = fromApi.readers;
            writers = fromApi.writers;
            return true;
        }

        return false;
    }

    @Path("resolve")
    @Produces("application/json")
    @POST
    public ResolutionResource resolve(@Context UriInfo uriInfo, @Context HttpHeaders headers) {

        checkUserRead(headers);

        int seconds = 1000;
        return new ResolutionResource( getPresignedURL(seconds), uriInfo.getBaseUri(), seconds );
    }

    private URI getPresignedURL(int seconds) {
        return api.getPresignedURL(seconds);
    }

    @Consumes("application/json")
    @Produces("application/json")
    @POST
    public ObjectResource update(@Context UriInfo uriInfo,
                                 @Context HttpHeaders headers,
                                 ObjectResource newRec) {

        if(populateFromAPI()) {

            checkUserWrite(headers);

            if (newRec.objectId != null && !objectId.equals(newRec.objectId)) {
                throw new IllegalArgumentException(String.format(
                        "Can't update the objectId (from \"%s\" to \"%s\")", objectId, newRec.objectId));
            }

            readers = setFrom(readers, newRec.readers);
            writers = setFrom(writers, newRec.writers);
            group = errorIfSet(group, newRec.group, "group");
            name = errorIfSet(name, newRec.name, "name");
            ownerId = setFrom(ownerId, newRec.ownerId);
            sizeEstimateBytes = errorIfSet(sizeEstimateBytes, newRec.sizeEstimateBytes, "sizeEstimateBytes");

            updateInAPI();
            return this;

        } else {

            /**
             * Registering a new object is a 'write' to the parent group, so we need to retrieve
             * the parent group and check its write permissions.
             */
            GroupResource parentGroup = api.getGroup(group);
            if(parentGroup == null) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(String.format("Couldn't find group %s", group)).build());
            }
            parentGroup.checkUserWrite(headers);

            newRec.objectId = objectId;
            newRec.group = group;
            api.updateObject(newRec);
            return newRec;
        }
    }

    private void updateInAPI() {
        api.updateObject(this);
    }

}
