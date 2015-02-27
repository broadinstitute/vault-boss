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

package org.genomebridge.boss.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.boss.http.models.ResolutionRequest;
import org.genomebridge.boss.http.resources.*;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.Random;

import static org.fest.assertions.api.Assertions.assertThat;

public class ObjectResourceAcceptanceTest extends AbstractTest {

    public static int FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();
    public static int CREATED = Response.Status.CREATED.getStatusCode();
    public static int GONE = Response.Status.GONE.getStatusCode();
    public static int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    public static int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    public static int OK = Response.Status.OK.getStatusCode();

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));


    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    @Test
    public void registerObjectTest() {
        String name = "Test Object";
        String owner = "carlyeks";
        Long sizeEstimate = 1010L;

        ClientResponse response = checkStatus( CREATED, createObject(name, owner, sizeEstimate) );
        String objectPath = checkHeader(response, "Location");

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created).isNotNull();
        assertThat(created.objectId).isNotNull();

        assertThat(objectPath).endsWith(created.objectId);
        assertThat(created.objectName).isEqualTo(name);
        assertThat(created.ownerId).isEqualTo(owner);
        assertThat(created.sizeEstimateBytes).isEqualTo(sizeEstimate);
        assertThat(created.readers).containsOnly("carlyeks", "testuser");
        assertThat(created.writers).containsOnly("carlyeks", "testuser");
    }

    @Test
    public void registerObjectAndDescribeTest() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED, createObject("Test Name", "tdanford", 1010L) );
        String objectPath = checkHeader(response, "Location");

        response = check200( get(client, objectPath) );

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(objectPath).endsWith(created.objectId);
        assertThat(created.objectName).describedAs("created object name").isEqualTo("Test Name");
        assertThat(created.sizeEstimateBytes).isEqualTo(1010L);
        assertThat(created.storagePlatform).isEqualTo("objectstore");
        assertThat(created.directoryPath).isNull();
        assertThat(created.writers).describedAs("created object writers").containsOnly("tdanford", "testuser");
        assertThat(created.readers).describedAs("created object readers").containsOnly("tdanford", "testuser");
    }

    @Test
    public void registerFilesystemObjectAndDescribeTest() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED,
                createObject("Test Name", "tdanford", "filesystem", "/my/path", 1010L) );
        String objectPath = checkHeader(response, "Location");

        response = check200( get(client, objectPath) );

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(objectPath).endsWith(created.objectId);
        assertThat(created.objectName).describedAs("created object name").isEqualTo("Test Name");
        assertThat(created.sizeEstimateBytes).isEqualTo(1010L);
        assertThat(created.storagePlatform).isEqualTo("filesystem");
        assertThat(created.directoryPath).isNotNull();
        assertThat(created.directoryPath).isEqualTo("/my/path");
        assertThat(created.writers).describedAs("created object writers").containsOnly("tdanford", "testuser");
        assertThat(created.readers).describedAs("created object readers").containsOnly("tdanford", "testuser");
    }

    @Test
    public void registerDescribeAndDeleteTest() {
        Client client = new Client();

        // until we have a mock object store, calling delete on an objectstore-object will fail, because
        // the system will reach out to the real objectstore and attempt to delete it. We'll cover that test
        // in the end-to-end integration tests.
        ClientResponse response = checkStatus( CREATED, createObject("Test Name", "tdanford", "filesystem", "/foo/bar", 1010L) );
        ObjectResource created = response.getEntity(ObjectResource.class);
        String objectPath = checkHeader(response, "Location");

        checkStatus( OK, get(client, objectPath) );
        checkStatus( OK, delete(client, objectPath));
        response = checkStatus( GONE, get(client, objectPath) );
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object with id %s has been deleted", created.objectId));
    }

    @Test
    public void testInvalidObjectDescribe() {
        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createObject("test object", "tdanford", 100L));
        ObjectResource created = response.getEntity(ObjectResource.class);
        String objectPath = checkHeader(response, "Location");
        String truncatedObjectPath = objectPath.substring(0, objectPath.length() - 1);
        String truncatedObjectId = created.objectId.substring(0, created.objectId.length() - 1);

        response = checkStatus( NOT_FOUND, get(client, truncatedObjectPath));

        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Couldn't find object with id %s", truncatedObjectId));
    }

    @Test
    public void setIllegalDeleteOnObject() {
        Client client = new Client();
        final String fakeUser = "fake_user";

        String objectPath = checkHeader(
                checkStatus( CREATED, createObject("Deletable", "tdanford", 100L )),
                "Location" );
        ClientResponse response = checkStatus(FORBIDDEN, delete(client, objectPath, fakeUser));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("User \"%s\" is not allowed WRITE access to resource with ACL [tdanford, testuser]", fakeUser));

        check200( get(client, objectPath) );
    }

    @Test
    public void testSetPermissionsOnObject() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED, createObject("changeable", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.readers = arrayAppend( rec.readers, "new_reader" );
        rec.writers = arrayAppend( rec.writers, "new_writer" );

        check200( post(client, objectPath, rec));

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectResource.class);

        assertThat(rec.readers).containsOnly("testuser", "tdanford", "new_reader");
        assertThat(rec.writers).containsOnly("testuser", "tdanford", "new_writer");
    }


    @Test
    public void setOwnerOnObject() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED, createObject("changeable", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.ownerId = "new_owner";

        check200( post(client, objectPath, rec));

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectResource.class);

        assertThat(rec.ownerId).isEqualTo("new_owner");
    }

    @Test
    public void setNotAllowedToSetNameOnObject() {
        /**
         * "It's illegal for a user to set the Name field on an already-registered object."
         */
        Client client = new Client();
        final String unchangeableName = "unchangeable";
        final String newName = "New Name";

        ClientResponse response = checkStatus(CREATED, createObject(unchangeableName, "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.objectName = newName;

        // It's illegal to change the name!
        response = checkStatus(BAD_REQUEST, post(client, objectPath, rec));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("objectName was different than previously set. Expected: %s; given: %s", unchangeableName, newName));

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectResource.class);

        // The name is unchanged.
        assertThat(rec.objectName).isEqualTo(unchangeableName);
    }

    @Test
    public void testIllegalSetPermissions() {
        Client client = new Client();
        final String fakeUser = "fake_user";

        ClientResponse response = checkStatus( CREATED, createObject("test object", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectResource rec = response.getEntity(ObjectResource.class);

        rec.readers = arrayAppend(rec.readers, "new_reader");

        // It's illegal, as the user 'fake_user', to update the readers field of the ObjectResource
        response = checkStatus(FORBIDDEN, post(client, objectPath, fakeUser, rec));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("User \"%s\" is not allowed WRITE access to resource with ACL [tdanford, testuser]", fakeUser));

        response = check200( get(client, objectPath) );

        rec = response.getEntity(ObjectResource.class);

        // The readers are unchanged.
        assertThat(rec.readers).containsOnly("testuser", "tdanford");
    }

    @Test
    public void testObjectResolve() {
        testObjectResolve(null, null);
    }

    @Test
    public void testObjectResolveWithContent() {
        testObjectResolve("application/octet-stream", "00000000000000000000000000000000");
    }

    private void testObjectResolve(String contentType, String contentMD5Hex) {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        ClientResponse response = checkStatus( CREATED, createObject("test object", "tdanford", 100L));
        String objectPath = checkHeader( response, "Location");
        ObjectResource created = response.getEntity(ObjectResource.class);

        ResolutionRequest req = new ResolutionRequest("GET", seconds, contentType, contentMD5Hex);

        response = post(client, objectPath + "/resolve", req);
        // If the user doesn't have a correct objectstore configuration, this is a typical symptom
        assertThat(response.getStatus()).overridingErrorMessage("Unexpected server error: Is your environment correctly configured for the S3 objectstore?").isNotEqualTo(INTERNAL_SERVER_ERROR);

        assertThat(response.getStatus()).isEqualTo(OK);

        ResolutionResource rec = response.getEntity(ResolutionResource.class);

        assertThat(rec).isNotNull();
        assertThat(rec.objectUrl.toString()).startsWith(
                String.format("https://genomebridge-variantstore-ci.s3.amazonaws.com/%s-",
                        created.objectId));
        assertThat(rec.validityPeriodSeconds).isEqualTo(seconds);
        assertThat(rec.contentType).isEqualTo(contentType);
        assertThat(rec.contentMD5Hex).isEqualTo(contentMD5Hex);
    }

    @Test
    public void testObjectResolveWithBadCharMD5Hex() {
        testObjectResolveBadRequest("application/octet-stream", "0000000000000000000000000000000g");
    }

    @Test
    public void testObjectResolveWithTruncatedMD5Hex() {
        testObjectResolveBadRequest("application/octet-stream", "000000000000000000000000000000  ");
    }

    @Test
    public void testObjectResolveWithShortMD5Hex() {
        testObjectResolveBadRequest("application/octet-stream", "000000000000000000000000000000");
    }

    @Test
    public void testObjectResolveWithLongMD5Hex() {
        testObjectResolveBadRequest("application/octet-stream", "0000000000000000000000000000000000");
    }

    private void testObjectResolveBadRequest(String contentType, String contentMD5Hex) {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        ClientResponse response = checkStatus( CREATED, createObject("test object", "tdanford", 100L));
        String objectPath = checkHeader( response, "Location");
        response.getEntity(ObjectResource.class);

        ResolutionRequest req = new ResolutionRequest("GET", seconds, contentType, contentMD5Hex);

        response = post(client, objectPath + "/resolve", req);
        // If the user doesn't have a correct objectstore configuration, this is a typical symptom
        assertThat(response.getStatus()).overridingErrorMessage("Unexpected server error: Is your environment correctly configured for the S3 objectstore?").isNotEqualTo(INTERNAL_SERVER_ERROR);

        checkStatus(BAD_REQUEST, response);
    }

    @Test
    public void testFilesystemObjectResolve() {
        Client client = new Client();
        ClientResponse response = checkStatus( CREATED,
                createObject("test fs object", "tdanford", "filesystem", "/path/to/file", 100L));
        String objectPath = checkHeader( response, "Location" );

        int seconds = 1000;
        ResolutionRequest req = new ResolutionRequest("GET", seconds);

        response = check200( post(client, objectPath + "/resolve", req) );

        ResolutionResource rr = response.getEntity(ResolutionResource.class);

        assertThat(rr).isNotNull();
        assertThat(rr.objectUrl.toString()).isEqualTo("file:///path/to/file");
        assertThat(rr.contentType).isNull();
        assertThat(rr.contentMD5Hex).isNull();
    }

    @Test
    public void testIllegalObjectResolve() {
        Client client = new Client();
        Random rand = new Random();
        final String fakeUser = "fake_user";

        int seconds = rand.nextInt(100) + 10;

        ClientResponse response = checkStatus(CREATED, createObject("test object", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ResolutionRequest req = new ResolutionRequest("GET", seconds);

        response = checkStatus(FORBIDDEN, post(client, objectPath + "/resolve", fakeUser, req));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("User \"%s\" is not allowed READ access to resource with ACL [tdanford, testuser]", fakeUser));
    }

    @Test
    public void testInvalidObjectResolveAndDelete() {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        ClientResponse response = checkStatus(CREATED, createObject("test object", "tdanford", 100L));
        ObjectResource created = response.getEntity(ObjectResource.class);
        String objectPath = checkHeader(response, "Location");
        String truncatedObjectPath = objectPath.substring(0, objectPath.length() - 1);
        String truncatedObjectId = created.objectId.substring(0, created.objectId.length() - 1);

        ResolutionRequest req = new ResolutionRequest("GET", seconds);
        response = checkStatus( NOT_FOUND, post(client, truncatedObjectPath + "/resolve", req));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Couldn't find object with id %s", truncatedObjectId));

        // confirm that we also can't delete it
        response = checkStatus(NOT_FOUND, delete(client, truncatedObjectPath));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Couldn't find object with id %s", truncatedObjectId));
    }

    @Test
    public void testDeletedObjectResolveAndDelete() {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        // until we have a mock object store, calling delete on an objectstore-object will fail, because
        // the system will reach out to the real objectstore and attempt to delete it. We'll cover that test
        // in the end-to-end integration tests.
        ClientResponse response = checkStatus(CREATED, createObject("Test Name", "tdanford", "filesystem", "/foo/bar", 1010L));
        ObjectResource created = response.getEntity(ObjectResource.class);
        String objectPath = checkHeader(response, "Location");

        checkStatus( OK, get(client, objectPath) );
        checkStatus( OK, delete(client, objectPath));
        ResolutionRequest req = new ResolutionRequest("GET", seconds);
        response = checkStatus( GONE, post(client, objectPath + "/resolve", req));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object with id %s has been deleted", created.objectId));

        // confirm that we can't re-delete it
        checkStatus(BAD_REQUEST, delete(client, objectPath));
    }
}
