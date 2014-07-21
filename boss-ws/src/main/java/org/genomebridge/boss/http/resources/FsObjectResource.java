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
import org.genomebridge.boss.http.service.DeregisteredObjectException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URL;

public class FsObjectResource extends AbstractResource {

    private BossAPI api;

    public String objectId;
    public URL objectUrl;
    public Long sizeEstimateBytes;
    public String name;
    public String ownerId;
    public String[] readers, writers;

    public FsObjectResource() {}

    public FsObjectResource(BossAPI api, String objectId) {
        this.api = api;
        this.objectId = objectId;
    }

    @DELETE
    public String delete() {
        try {
            deleteFromAPI();
            return this.objectId;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private void deleteFromAPI() {
        api.deregisterFsObject(this);
    }

    @Produces("application/json")
    @GET
    public FsObjectResource describe() {
        try {
            populateFromAPI();
            return this;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.GONE);
        }
    }

    private boolean populateFromAPI() {
        FsObjectResource fromApi = api.getFsObject(objectId);

        if(fromApi != null) {
            objectUrl = fromApi.objectUrl;
            sizeEstimateBytes = fromApi.sizeEstimateBytes;
            name = fromApi.name;
            ownerId = fromApi.ownerId;
            readers = fromApi.readers;
            writers = fromApi.writers;
            return true;
        }

        return false;
    }

    @Consumes("application/json")
    @Produces("application/json")
    @POST
    public FsObjectResource update(@Context UriInfo uriInfo, FsObjectResource newRec) {

        if(populateFromAPI()) {
            if (newRec.objectId != null && !objectId.equals(newRec.objectId)) {
                throw new IllegalArgumentException(String.format(
                        "Can't update the objectId (from \"%s\" to \"%s\")", objectId, newRec.objectId));
            }

            objectUrl = errorIfSet(objectUrl, newRec.objectUrl);
            readers = setFrom(readers, newRec.readers);
            writers = setFrom(writers, newRec.writers);
            name = errorIfSet(name, newRec.name);
            ownerId = setFrom(ownerId, newRec.ownerId);
            sizeEstimateBytes = errorIfSet(sizeEstimateBytes, newRec.sizeEstimateBytes);

            updateInAPI();
            return this;

        } else {

            newRec.objectId = uriInfo.getRequestUri().toString();
            api.updateFsObject(newRec);
            return newRec;
        }
    }

    private void updateInAPI() {
        api.updateFsObject(this);
    }

}
