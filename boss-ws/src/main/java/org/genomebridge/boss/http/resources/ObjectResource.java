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
import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.google.inject.Inject;
import com.sun.jersey.api.NotFoundException;
import org.apache.log4j.Logger;
import org.genomebridge.boss.http.models.ResolutionRequest;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DeregisteredObjectException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("objects/{objectId}")
@JsonInclude(Include.NON_NULL)
public class ObjectResource extends PermissionedResource {

    private BossAPI api;

    public String objectId;
    public String objectName;
    public String storagePlatform;
    public Long sizeEstimateBytes;
    public String ownerId;
    public String[] readers, writers;

    public ObjectResource() {
    }

    @Inject
    public ObjectResource(BossAPI api) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    public ObjectResource describe(@PathParam("objectId") String objectId,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo uriInfo) {
        if (!populateFromAPI(objectId)) {
            throw new NotFoundException(String.format("Couldn't find object with id %s", objectId));
        }
        
        checkUserRead(headers);

        return this;
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger(objectId);
    }

    public void checkUserRead( String user ) { checkUser(user, "READ", readers); }
    public void checkUserWrite( String user ) { checkUser(user, "WRITE", writers); }

    private boolean populateFromAPI(String objectId) {

        ObjectResource rec = api.getObject(objectId);

        if(rec != null) {
            this.objectId = rec.objectId;
            ownerId = rec.ownerId;
            objectName = rec.objectName;
            storagePlatform = rec.storagePlatform;
            sizeEstimateBytes = rec.sizeEstimateBytes;
            readers = rec.readers;
            writers = rec.writers;

            return true;
        }

        return false;
    }

    @Path("resolve")
    @Produces("application/json")
    @POST
    public ResolutionResource resolve(
            @PathParam("objectId") String objectId,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            ResolutionRequest request) {

        if(!populateFromAPI(objectId)) { throw new NotFoundException(objectId); }
        checkUserRead(headers);

        /*
        If we're looking at a filesystem object, then a RESOLVE request isn't a well-formed
        request.
         */
        if(!storagePlatform.equals("objectstore")) {
            String msg = String.format("Can't resolve a non-objectstore object %s", objectId);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg).build());
        }

        try {
            long timeoutMillis = 1000L * request.validityPeriodSeconds;
            org.genomebridge.boss.http.objectstore.HttpMethod method = org.genomebridge.boss.http.objectstore.HttpMethod.valueOf(request.httpMethod);

            return new ResolutionResource(
                    getPresignedURL(objectId, method, timeoutMillis),
                    uriInfo.getBaseUri(),
                    request.validityPeriodSeconds);

        } catch(IllegalArgumentException e) {
            String msg = String.format("Error in request, with message \"%s\"", e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg).build());
        }
    }

    private URI getPresignedURL(String objId, org.genomebridge.boss.http.objectstore.HttpMethod method, long millis) {
        return api.getPresignedURL(objId, method, millis);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ObjectResource update(@PathParam("objectId") String objectId,
                                 @Context HttpHeaders header,
                                 @Context UriInfo info,
                                 ObjectResource newrec) {

        if(populateFromAPI(objectId)) {
            checkUserWrite(header);

            this.objectId = errorIfSet(objectId, newrec.objectId, "objectId");
            this.objectName = errorIfSet(objectName, newrec.objectName, "objectName");
            this.ownerId = setFrom(ownerId, newrec.ownerId);
            this.storagePlatform = errorIfSet(storagePlatform, newrec.storagePlatform, "storagePlatform");
            this.sizeEstimateBytes = errorIfSet(
                    sizeEstimateBytes, newrec.sizeEstimateBytes, "sizeEstimateBytes");
            this.readers = setFrom(readers, newrec.readers);
            this.writers = setFrom(writers, newrec.writers);

            updateInAPI(objectId);

            return this;
        } else {

            api.updateObject(objectId, newrec);
            return newrec;
        }
    }

    private void updateInAPI(String objectId) {
        api.updateObject(objectId, this);
    }

    @DELETE
    public String delete(@PathParam("objectId") String objectId,
                         @Context HttpHeaders headers) {
        try {
            populateFromAPI(objectId);
            checkUserWrite(headers);
            deleteFromAPI(objectId);
            return this.objectId;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private void deleteFromAPI(String objectId) {
        api.deleteObject(objectId);
    }
}
