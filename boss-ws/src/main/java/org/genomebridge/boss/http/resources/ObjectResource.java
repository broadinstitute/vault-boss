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

import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;
import org.genomebridge.boss.http.service.BossAPI.ResolveRequest;
import org.genomebridge.boss.http.service.BossAPI.ResolveResponse;

import javax.ws.rs.*;
import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@Path("objects/{objectId}")
@JsonInclude(Include.NON_NULL)
public class ObjectResource extends AbstractResource {

    public ObjectResource( BossAPI api ) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    public ObjectDesc describe(@PathParam("objectId") String objectId,
                               @HeaderParam("REMOTE_USER") String userName) {
        ObjectDesc desc = new ObjectDesc();
        ErrorDesc err = api.getObject(objectId, userName, desc);
        if ( err != null )
            throwWAE(err);
        return desc;
    }

    @Path("resolve")
    @Produces("application/json")
    @POST
    public ResolveResponse resolve(@PathParam("objectId") String objectId,
                                   @HeaderParam("REMOTE_USER") String userName,
                                   ResolveRequest req) {
        ResolveResponse resp = new ResolveResponse();
        ErrorDesc err = api.resolveObject(objectId, userName, req, resp);
        if ( err != null )
            throwWAE(err);
        return resp;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ObjectDesc update(@PathParam("objectId") String objectId,
                             @HeaderParam("REMOTE_USER") String userName,
                             ObjectDesc desc) {
        ErrorDesc err = api.updateObject(desc,objectId,userName);
        if ( err != null )
            throwWAE(err);
        return desc;
    }

    @DELETE
    public String delete(@PathParam("objectId") String objectId,
                         @HeaderParam("REMOTE_USER") String userName) {
        ErrorDesc err = api.deleteObject(objectId,userName);
        if ( err != null )
            throwWAE(err);
        return objectId;
    }

    private BossAPI api;
}
