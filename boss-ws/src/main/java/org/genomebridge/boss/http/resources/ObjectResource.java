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

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URL;

public class ObjectResource extends AbstractResource {

    private BossAPI api;

    public String objectId;
    public URL objectUrl;
    public Long sizeEstimateBytes;
    public String name;
    public String ownerId;
    public String[] readers, writers;

    public ObjectResource() {}

    public ObjectResource(BossAPI api, String objectId) {
        this.api = api;
        this.objectId = objectId;
    }

    @Produces("application/json")
    @GET
    public ObjectResource describe() {
        populateFromAPI();
        return this;
    }

    private void populateFromAPI() {
        ObjectResource fromApi = api.getObject(objectId);

        objectUrl = fromApi.objectUrl;
        sizeEstimateBytes = fromApi.sizeEstimateBytes;
        name = fromApi.name;
        ownerId = fromApi.ownerId;
        readers = fromApi.readers;
        writers = fromApi.writers;
    }

    @Path("resolve")
    @Produces("application/json")
    @POST
    public ResolutionResource resolve(@Context UriInfo uriInfo) {
        int seconds = 1000;
        return new ResolutionResource( getPresignedURL(seconds), uriInfo.getBaseUri(), seconds );
    }

    private URI getPresignedURL(int seconds) {
        return api.getPresignedURL(seconds);
    }

    @Consumes("application/json")
    @Produces("application/json")
    @POST
    public ObjectResource update(ObjectResource newRec) {

        if(newRec.objectId != null && !objectId.equals(newRec.objectId)) {
            throw new IllegalArgumentException(String.format(
                    "Can't update the objectId (from \"%s\" to \"%s\")", objectId, newRec.objectId));
        }

        objectUrl = setFrom(objectUrl, newRec.objectUrl);
        readers = setFrom(readers, newRec.readers);
        writers = setFrom(writers, newRec.writers);
        name = setFrom(name, newRec.name);
        ownerId = setFrom(ownerId, newRec.ownerId);
        sizeEstimateBytes = setFrom(sizeEstimateBytes, newRec.sizeEstimateBytes);

        updateInAPI();

        return this;
    }

    private void updateInAPI() {
        api.updateObject(this);
    }

}
