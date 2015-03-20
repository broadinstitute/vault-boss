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

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;
import org.genomebridge.boss.http.service.BossAPI.CopyRequest;
import org.genomebridge.boss.http.service.BossAPI.CopyResponse;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;
import org.genomebridge.boss.http.service.BossAPI.ResolveRequest;
import org.genomebridge.boss.http.service.BossAPI.ResolveResponse;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.HttpURLConnection;
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

        ObjectDesc created = response.getEntity(ObjectDesc.class);

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

        ObjectDesc created = response.getEntity(ObjectDesc.class);

        assertThat(objectPath).endsWith(created.objectId);
        assertThat(created.objectName).describedAs("created object name").isEqualTo("Test Name");
        assertThat(created.sizeEstimateBytes).isEqualTo(1010L);
        assertThat(created.storagePlatform).isEqualTo(StoragePlatform.LOCALSTORE.getValue());
        assertThat(created.directoryPath).isNull();
        assertThat(created.writers).describedAs("created object writers").containsOnly("tdanford", "testuser");
        assertThat(created.readers).describedAs("created object readers").containsOnly("tdanford", "testuser");
    }

    @Test
    public void registerFilesystemObjectAndDescribeTest() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED,
                createObject("Test Name", "tdanford", StoragePlatform.OPAQUEURI.getValue(), "/my/path", 1010L) );
        String objectPath = checkHeader(response, "Location");

        response = check200( get(client, objectPath) );

        ObjectDesc created = response.getEntity(ObjectDesc.class);

        assertThat(objectPath).endsWith(created.objectId);
        assertThat(created.objectName).describedAs("created object name").isEqualTo("Test Name");
        assertThat(created.sizeEstimateBytes).isEqualTo(1010L);
        assertThat(created.storagePlatform).isEqualTo(StoragePlatform.OPAQUEURI.getValue());
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
        ClientResponse response = checkStatus( CREATED, createObject("Test Name", "tdanford", StoragePlatform.OPAQUEURI.getValue(), "/foo/bar", 1010L) );
        ObjectDesc created = response.getEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, "Location");

        checkStatus( OK, get(client, objectPath) );
        checkStatus( OK, delete(client, objectPath));
        response = checkStatus( GONE, get(client, objectPath) );
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object %s was deleted.", created.objectId));
    }

    @Test
    public void testInvalidObjectDescribe() {
        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createObject("test object", "tdanford", 100L));
        ObjectDesc created = response.getEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, "Location");
        String truncatedObjectPath = objectPath.substring(0, objectPath.length() - 1);
        String truncatedObjectId = created.objectId.substring(0, created.objectId.length() - 1);

        response = checkStatus( NOT_FOUND, get(client, truncatedObjectPath));

        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object %s not found.", truncatedObjectId));
    }

    @Test
    public void setIllegalDeleteOnObject() {
        Client client = new Client();
        final String fakeUser = "fake_user";

        String objectPath = checkHeader(
                checkStatus( CREATED, createObject("Deletable", "tdanford", 100L )),
                "Location" );
        ClientResponse response = checkStatus(FORBIDDEN, delete(client, objectPath, fakeUser));
        String objectId = objectPath.substring(objectPath.length()-36,objectPath.length());
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("No read permission for %s by %s.", objectId, fakeUser));

        check200( get(client, objectPath) );
    }

    @Test
    public void testSetPermissionsOnObject() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED, createObject("changeable", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectDesc rec = response.getEntity(ObjectDesc.class);
        assertThat(rec).isNotNull();

        rec.readers = arrayAppend( rec.readers, "new_reader" );
        rec.writers = arrayAppend( rec.writers, "new_writer" );

        check200( post(client, objectPath, rec));

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectDesc.class);

        assertThat(rec.readers).containsOnly("testuser", "tdanford", "new_reader");
        assertThat(rec.writers).containsOnly("testuser", "tdanford", "new_writer");
    }


    @Test
    public void setOwnerOnObject() {
        Client client = new Client();

        ClientResponse response = checkStatus( CREATED, createObject("changeable", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectDesc rec = response.getEntity(ObjectDesc.class);
        assertThat(rec).isNotNull();

        rec.ownerId = "new_owner";

        check200( post(client, objectPath, rec));

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectDesc.class);

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

        ObjectDesc rec = response.getEntity(ObjectDesc.class);
        assertThat(rec).isNotNull();

        rec.objectName = newName;

        // It's illegal to change the name!
        response = checkStatus(BAD_REQUEST, post(client, objectPath, rec));
        assertThat(response.getEntity(String.class))
                .isEqualTo("ObjectName cannot be modified.");

        response = check200( get(client, objectPath));

        rec = response.getEntity(ObjectDesc.class);

        // The name is unchanged.
        assertThat(rec.objectName).isEqualTo(unchangeableName);
    }

    @Test
    public void testIllegalSetPermissions() {
        Client client = new Client();
        final String fakeUser = "fake_user";

        ClientResponse response = checkStatus( CREATED, createObject("test object", "tdanford", 100L));
        String objectPath = checkHeader(response, "Location");

        ObjectDesc rec = response.getEntity(ObjectDesc.class);

        rec.readers = arrayAppend(rec.readers, "new_reader");

        // It's illegal, as the user 'fake_user', to update the readers field of the ObjectDesc
        response = checkStatus(FORBIDDEN, post(client, objectPath, fakeUser, rec));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("No write permission for %s by %s.", rec.objectId, fakeUser));

        response = check200( get(client, objectPath) );

        rec = response.getEntity(ObjectDesc.class);

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
        ObjectDesc desc = response.getEntity(ObjectDesc.class);

        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;
        req.contentType = contentType;
        req.contentMD5Hex = contentMD5Hex;

        response = post(client, objectPath + "/resolve", req);
        // If the user doesn't have a correct objectstore configuration, this is a typical symptom
        assertThat(response.getStatus()).overridingErrorMessage("Unexpected server error: Is your environment correctly configured for the S3 objectstore?").isNotEqualTo(INTERNAL_SERVER_ERROR);

        assertThat(response.getStatus()).isEqualTo(OK);

        ResolveResponse rec = response.getEntity(ResolveResponse.class);

        assertThat(rec).isNotNull();
        ObjectStoreConfiguration config = RULE.getConfiguration().getLocalStoreConfiguration();
        String urlToExpect = config.endpoint + '/' + config.bucket + '/' + desc.objectId;
        assertThat(rec.objectUrl.toString()).startsWith(urlToExpect);
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
        response.getEntity(ObjectDesc.class);

        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;
        req.contentType = contentType;
        req.contentMD5Hex = contentMD5Hex;

        response = post(client, objectPath + "/resolve", req);
        // If the user doesn't have a correct objectstore configuration, this is a typical symptom
        assertThat(response.getStatus()).overridingErrorMessage("Unexpected server error: Is your environment correctly configured for the S3 objectstore?").isNotEqualTo(INTERNAL_SERVER_ERROR);

        checkStatus(BAD_REQUEST, response);
    }

    @Test
    public void testFilesystemObjectResolve() {
        Client client = new Client();
        ClientResponse response = checkStatus( CREATED,
                createObject("test fs object", "tdanford", "opaqueURI", "file:///path/to/file", 100L));
        String objectPath = checkHeader( response, "Location" );

        int seconds = 1000;
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;

        response = check200( post(client, objectPath + "/resolve", req) );

        ResolveResponse rr = response.getEntity(ResolveResponse.class);

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
        ObjectDesc desc = response.getEntity(ObjectDesc.class);
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;

        response = checkStatus(FORBIDDEN, post(client, objectPath + "/resolve", fakeUser, req));
        assertThat(response.getEntity(String.class))
                .isEqualTo(String.format("No read permission for %s by %s.", desc.objectId, fakeUser));
    }

    @Test
    public void testInvalidObjectResolveAndDelete() {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        ClientResponse response = checkStatus(CREATED, createObject("test object", "tdanford", 100L));
        ObjectDesc created = response.getEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, "Location");
        String truncatedObjectPath = objectPath.substring(0, objectPath.length() - 1);
        String truncatedObjectId = created.objectId.substring(0, created.objectId.length() - 1);

        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;
        response = checkStatus( NOT_FOUND, post(client, truncatedObjectPath + "/resolve", req));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object %s not found.", truncatedObjectId));

        // confirm that we also can't delete it
        response = checkStatus(NOT_FOUND, delete(client, truncatedObjectPath));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object %s not found.", truncatedObjectId));
    }

    @Test
    public void testDeletedObjectResolveAndDelete() {
        Client client = new Client();
        Random rand = new Random();

        int seconds = rand.nextInt(100) + 10;

        // until we have a mock object store, calling delete on an objectstore-object will fail, because
        // the system will reach out to the real objectstore and attempt to delete it. We'll cover that test
        // in the end-to-end integration tests.
        ClientResponse response = checkStatus(CREATED, createObject("Test Name", "tdanford", StoragePlatform.OPAQUEURI.getValue(), "/foo/bar", 1010L));
        ObjectDesc created = response.getEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, "Location");

        checkStatus( OK, get(client, objectPath) );
        checkStatus( OK, delete(client, objectPath));
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = seconds;
        response = checkStatus( GONE, post(client, objectPath + "/resolve", req));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format("Object %s was deleted.", created.objectId));

        // confirm that we can't re-delete it
        checkStatus(NOT_FOUND, delete(client, objectPath));
    }

    // can't do this right now -- we don't want to put valid objectstore credentials into github.
    // to run this test, copy a valid boss-config.yml to src/test/resources, and put the bossdev.p12
    // key file wherever boss-config.yml says it is.
    //@Test
    public void testActuallyStoringSomeContent() {
        Client client = new Client();

        // create a cloudStore object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "testuser";
        obj.objectName = "someObject";
        obj.readers = arraySet( "testuser" );
        obj.writers = arraySet( "testuser" );
        obj.sizeEstimateBytes = 13L;
        obj.storagePlatform = StoragePlatform.CLOUDSTORE.getValue();
        ClientResponse response = checkStatus(CREATED, post(client,objectsPath(),obj));
        String objectPath = checkHeader(response, "Location");

        // resolve the object for a PUT
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.PUT;
        req.validityPeriodSeconds = 120;
        req.contentType = MediaType.TEXT_PLAIN;
        response = checkStatus(OK, post(client,objectPath+"/resolve","testuser",req));
        ResolveResponse resp = response.getEntity(ResolveResponse.class);

        // write some content to the signed URL
        String putContent = "Some content.";
        response = client.resource(resp.objectUrl.toString())
                         .type(MediaType.TEXT_PLAIN)
                         .put(ClientResponse.class,putContent);
        assertThat(response.getStatus()).isEqualTo(OK);

        // resolve the object for a GET
        req.httpMethod = HttpMethod.GET;
        response = checkStatus(OK, post(client,objectPath+"/resolve","testuser",req));
        resp = response.getEntity(ResolveResponse.class);

        // get the data from the signed URL
        response = client.resource(resp.objectUrl.toString())
                         .type(MediaType.TEXT_PLAIN)
                         .get(ClientResponse.class);
        assertThat(response.getStatus()).isEqualTo(OK);
        String getContent = response.getEntity(String.class);

        // make sure we got back what we wrote
        assertThat(getContent).isEqualTo(putContent);

        // clean up by deleting object
        checkStatus(OK,delete(client,objectPath));
    }

    // can't do this right now -- we don't want to put valid objectstore credentials into github.
    // to run this test, copy a valid boss-config.yml to src/test/resources, and put the bossdev.p12
    // key file wherever boss-config.yml says it is.
    //@Test
    public void testCopy() throws Exception {
        Client client = new Client();

        // create a cloudStore object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "testuser";
        obj.objectName = "copiedObject";
        obj.readers = arraySet( "testuser" );
        obj.writers = arraySet( "testuser" );
        obj.sizeEstimateBytes = 13L;
        obj.storagePlatform = StoragePlatform.CLOUDSTORE.getValue();
        ClientResponse response = checkStatus(CREATED, post(client,objectsPath(),obj));
        String objectPath = checkHeader(response, "Location");

        // here's a test object in another bucket to copy
        String locationToCopy = "/broad-dsde-dev-public/NA12878/uBam_21.bam";

        // get a signed URL for copying the test object to our new object
        CopyRequest copyReq = new CopyRequest();
        copyReq.locationToCopy = locationToCopy;
        copyReq.validityPeriodSeconds = 5;
        CopyResponse copyResp = checkStatus(OK,
                                            post(client,objectPath+"/copy","testuser",copyReq))
                                        .getEntity(CopyResponse.class);

        // ask GCS to do the copy
        // Jersey is too smart to send an empty PUT
        HttpURLConnection conn = (HttpURLConnection)copyResp.uri.toURL().openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod(HttpMethod.PUT);
        conn.setRequestProperty("x-goog-copy-source",locationToCopy);
        conn.getOutputStream().close();
        int respCode = conn.getResponseCode();
        assertThat(respCode).isEqualTo(OK);
        conn.disconnect();

        // resolve the object for a GET
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.contentType = MediaType.APPLICATION_OCTET_STREAM;
        req.validityPeriodSeconds = 5;
        response = checkStatus(OK, post(client,objectPath+"/resolve","testuser",req));
        ResolveResponse resolveResp = response.getEntity(ResolveResponse.class);

        // get the data from the signed URL
        response = client.resource(resolveResp.objectUrl.toString())
                         .type(MediaType.APPLICATION_OCTET_STREAM)
                         .get(ClientResponse.class);
        assertThat(response.getStatus()).isEqualTo(OK);

        // see that there are some bytes there
        byte[] content = response.getEntity(byte[].class);
        assertThat(content.length).isGreaterThan(155*1024*1024);

        // clean up by deleting object
        checkStatus(OK,delete(client,objectPath));
    }
}
