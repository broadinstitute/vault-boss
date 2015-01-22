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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.jersey.api.NotFoundException;

import org.apache.log4j.Logger;
import org.genomebridge.boss.http.models.ResolutionRequest;
import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.objectstore.ObjectStoreException;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPIProvider;
import org.genomebridge.boss.http.service.DeregisteredObjectException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.DatatypeConverter;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@Path("objects/{objectId}")
@JsonInclude(Include.NON_NULL)
public class ObjectResource extends PermissionedResource {

    private BossAPI api;

    public String objectId;
    public String objectName;
    public String storagePlatform;
    public String directoryPath;
    public Long sizeEstimateBytes;
    public String ownerId;
    public String[] readers, writers;

    @JsonIgnore
    public String active;
    @JsonIgnore
    public String createdBy;
    @JsonIgnore
    public Timestamp createDate;
    @JsonIgnore
    public Timestamp modifyDate;
    @JsonIgnore
    public Timestamp resolveDate;
    @JsonIgnore
    public Timestamp deleteDate;

    public ObjectResource() {
        this.api = BossAPIProvider.getInstance().getApi();
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

    @JsonIgnore
    public boolean isObjectStoreObject() { return (StoragePlatform.OBJECTSTORE.getValue()).equals( this.storagePlatform ); }
    @JsonIgnore
    public boolean isFilesystemObject() { return (StoragePlatform.FILESYSTEM.getValue()).equals( this.storagePlatform ); }

    @JsonIgnore
    public String testValidity() {
        ArrayList<String> errors = new ArrayList<String>();
        if ( objectName == null ) errors.add("objectName cannot be null");
        if ( ownerId == null ) errors.add("ownerId cannot be null");
        if ( storagePlatform == null ) errors.add("storagePlatform cannot be null");
        else if ( !storagePlatform.equals(StoragePlatform.OBJECTSTORE.getValue()) ) {
            if ( !storagePlatform.equals(StoragePlatform.FILESYSTEM.getValue()) )
                errors.add("storagePlatform must be either " + StoragePlatform.OBJECTSTORE.getValue() +
                        " or " + StoragePlatform.FILESYSTEM.getValue());
            else if ( directoryPath == null)
                errors.add("directoryPath must be supplied for filesystem objects");
        }
        String errMsg = null;
        if ( !errors.isEmpty() ) {
            errMsg = "";
            String sep = "";
            for ( String err : errors ) {
                errMsg += sep + err;
                sep = "; ";
            }
        }
        return errMsg;
    }

    private boolean populateFromAPI(String objectId) {

        ObjectResource rec = api.getObject(objectId);

        if (rec != null) {
            this.objectId = rec.objectId;
            ownerId = rec.ownerId;
            objectName = rec.objectName;
            storagePlatform = rec.storagePlatform;
            sizeEstimateBytes = rec.sizeEstimateBytes;

            if(storagePlatform.equals("filesystem")) {
                directoryPath = rec.directoryPath;
            }

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

        try {
            long timeoutMillis = 1000L * request.validityPeriodSeconds;
            org.genomebridge.boss.http.objectstore.HttpMethod method = org.genomebridge.boss.http.objectstore.HttpMethod.valueOf(request.httpMethod);

            byte[] contentMD5 = null;
            if (request.contentMD5Hex != null) {
                if (request.contentMD5Hex.length() != 32) {
                    throw new IllegalArgumentException("MD5 must be 32 hexadecimal characters long");
                }
                contentMD5 = DatatypeConverter.parseHexBinary(request.contentMD5Hex);
                if (contentMD5.length != 16) {
                    throw new IllegalArgumentException("MD5 must be 16 bytes long");
                }
            }

            URI presignedURL =
                    isObjectStoreObject() ?
                            getPresignedURL(objectId, method, timeoutMillis, request.contentType, contentMD5) :
                            URI.create(String.format("file://%s", directoryPath));

            return new ResolutionResource(
                    presignedURL,
                    request.validityPeriodSeconds,
                    request.contentType,
                    request.contentMD5Hex);

        } catch(IllegalArgumentException e) {
            String msg = String.format("Error in request, with message \"%s\"", e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg).build());
        }
    }

    private URI getPresignedURL(String objId, org.genomebridge.boss.http.objectstore.HttpMethod method, long millis,
                                String contentType, byte[] contentMD5) {
        return api.getPresignedURL(objId, method, millis, contentType, contentMD5);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ObjectResource update(@PathParam("objectId") String objectId,
                                 @Context HttpHeaders header,
                                 @Context UriInfo info,
                                 ObjectResource newrec) {

        if(!populateFromAPI(objectId)) { throw new NotFoundException(objectId); }

        checkUserWrite(header);

        this.objectId = errorIfSet(objectId, newrec.objectId, "objectId");
        this.objectName = errorIfSet(objectName, newrec.objectName, "objectName");
        this.ownerId = setFrom(ownerId, newrec.ownerId);
        this.storagePlatform = errorIfSet(storagePlatform, newrec.storagePlatform, "storagePlatform");
        this.sizeEstimateBytes = errorIfSet(
                sizeEstimateBytes, newrec.sizeEstimateBytes, "sizeEstimateBytes");
        this.directoryPath = errorIfSet(
                directoryPath, newrec.directoryPath, "directoryPath");
        this.readers = setFrom(readers, newrec.readers);
        this.writers = setFrom(writers, newrec.writers);

        api.updateObject(this);

        return this;
    }

    @DELETE
    public String delete(@PathParam("objectId") String objectId,
                         @Context HttpHeaders headers) {
        try {
            populateFromAPI(objectId);
            checkUserWrite(headers);
            deleteFromAPI(this);

            return this.objectId;

        } catch(DeregisteredObjectException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (ObjectStoreException ose) {
            logger().error("Error deleting object '" + objectId + "' from object store: " + ose.getLocalizedMessage(), ose);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private void deleteFromAPI(ObjectResource rec) {
        api.deleteObject(rec);
    }
}
