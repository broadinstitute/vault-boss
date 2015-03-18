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

import java.util.UUID;
import java.util.List;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class AllObjectsAcceptanceTest extends AbstractTest {

    public static int OK = ClientResponse.Status.OK.getStatusCode();
    public static int CREATED = ClientResponse.Status.CREATED.getStatusCode();
    public static int BAD_REQUEST = ClientResponse.Status.BAD_REQUEST.getStatusCode();
    public static int NOT_FOUND = ClientResponse.Status.NOT_FOUND.getStatusCode();
    public static int GONE = ClientResponse.Status.GONE.getStatusCode();

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));


    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testObjectStoreObjectCreation() {
        /**
         * "The user should be able to create a new ObjectDesc by POSTing to objects"
         */

        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createObject("Name", "tdanford", 500L));

        String location = checkHeader(response, "Location");

        response = check200( get(client, location) );

        ObjectDesc rec = response.getEntity(ObjectDesc.class);

        assertThat(rec).isNotNull();
        assertThat(rec.objectName).isEqualTo("Name");
        assertThat(rec.ownerId).isEqualTo("tdanford");
        assertThat(rec.sizeEstimateBytes).isEqualTo(500L);
    }

    @Test
    public void testNullNameObjectCreation() {
        /**
         * "The user should not be able to create an object with a null objectName"
         */
        ObjectDesc rec = fixture();
        rec.objectName = null;
        ClientResponse response = checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(messages.get("objectValidation")+'.');
    }

    @Test
    public void testNullStoragePlatformObjectCreation() {
        /**
         * "The user should not be able to create an object with a null storagePlatform"
         */
        ObjectDesc rec = fixture();
        rec.storagePlatform = null;
        ClientResponse response = checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(messages.get("storagePlatformValidation")+'.');
    }

    @Test
    public void testBadStoragePlatformObjectCreation() {
        /**
         * "The user should not be able to create an object with a bogus storagePlatform"
         */
        ObjectDesc rec = fixture();
        rec.storagePlatform = "xyzzy";
        ClientResponse response = checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format(messages.get("storagePlatformOptions"),
                StoragePlatform.CLOUDSTORE.getValue(),
                StoragePlatform.LOCALSTORE.getValue(),
                StoragePlatform.OPAQUEURI.getValue())+'.');
    }

    @Test
    public void testNoPathFilesystemObjectCreation() {
        /**
         * "The user should not be able to create an opaqueURI object without a directoryPath"
         */
        ObjectDesc rec = fixture();
        rec.directoryPath = null;
        ClientResponse response = checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format(messages.get("directoryPathToSupply"),"opaqueURI") + '.');
    }

    @Test
    public void testNullOwnerObjectCreation() {
        /**
         * "The user should not be able to create an object with a null ownerId"
         */
        ObjectDesc rec = fixture();
        rec.ownerId = null;
        ClientResponse response = checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(messages.get("ownerIdValidation")+'.');
    }

    @Test
    public void testBackdoorObjectCreation() {
        /**
         * "The user should not be able to create an object via an update"
         */
        ObjectDesc rec = fixture();
        Client client = new Client();
        final String fakeObjectId = "xyzzy";

        ClientResponse response = checkStatus(NOT_FOUND, post(client, String.format("%s/%s", objectsPath(), fakeObjectId), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format(messages.get("objectNotFound"), fakeObjectId));

        // check that this is also true for deleted objects

        // until we have a mock object store, calling delete on an objectstore-object will fail, because
        // the system will reach out to the real objectstore and attempt to delete it. We'll cover that test
        // in the end-to-end integration tests.
        response = checkStatus(CREATED, createObject("Name", "tdanford", StoragePlatform.OPAQUEURI.getValue(), "file:///path/to/file", 500L));
        ObjectDesc created = response.getEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, "Location");

        checkStatus(OK, get(client, objectPath));
        checkStatus(OK, delete(client, objectPath));
        response = checkStatus( GONE, post(client, String.format("%s/%s", objectsPath(), created.objectId), rec));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format(String.format(messages.get("objectDeleted"), created.objectId)));
        response = checkStatus( GONE, get(client, objectPath));
        assertThat(response.getEntity(String.class)).isEqualTo(String.format(messages.get("objectDeleted"), created.objectId));
    }

    @Test
    public void testFindByName() {
        Client client = new Client();
        ObjectDesc rec = fixture();
        rec.objectName = UUID.randomUUID().toString();
        String queryURL = objectsPath()+"?name="+rec.objectName;

        // shouldn't be any objects with our unique name yet
        checkStatus(NOT_FOUND,get(client,queryURL,"me"));

        // make an object with our name, and see if we can find it
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        ClientResponse response = checkStatus(OK,get(client,queryURL,"me"));
        GenericType<List<ObjectDesc>> genTyp = new GenericType<List<ObjectDesc>>() {};
        List<ObjectDesc> recs = response.getEntity(genTyp);
        assertThat(recs.size()).isEqualTo(1);
        assertThat(recs.get(0).objectName).isEqualTo(rec.objectName);

        // make another one, and see that we find both
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.getEntity(genTyp);
        assertThat(recs.size()).isEqualTo(2);
        assertThat(recs.get(0).objectName).isEqualTo(rec.objectName);
        assertThat(recs.get(1).objectName).isEqualTo(rec.objectName);

        // make one we can't see due to permissions, and make sure we still find just two
        rec.readers = arraySet("him","her");
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.getEntity(genTyp);
        assertThat(recs.size()).isEqualTo(2);

        // delete one of them and make sure we find just one
        checkStatus(OK,delete(client,objectsPath()+"/"+recs.get(0).objectId,"me"));
        String expectedId = recs.get(1).objectId;
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.getEntity(genTyp);
        assertThat(recs.size()).isEqualTo(1);
        assertThat(recs.get(0).objectId).isEqualTo(expectedId);
    }
}
