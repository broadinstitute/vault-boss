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

import org.genomebridge.boss.http.resources.ObjectResource;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class AllObjectsAcceptanceTest extends AbstractTest {

    public static int OK = ClientResponse.Status.OK.getStatusCode();
    public static int CREATED = ClientResponse.Status.CREATED.getStatusCode();
    public static int BAD_REQUEST = ClientResponse.Status.BAD_REQUEST.getStatusCode();
    public static int NOT_FOUND = ClientResponse.Status.NOT_FOUND.getStatusCode();

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
         * "The user should be able to create a new ObjectResource by POSTing to objects"
         */

        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createObject("Name", "tdanford", 500L));

        String location = checkHeader(response, "Location");

        response = check200( get(client, location) );

        ObjectResource rec = response.getEntity(ObjectResource.class);

        assertThat(rec).isNotNull();
        assertThat(rec.objectName).isEqualTo("Name");
        assertThat(rec.ownerId).isEqualTo("tdanford");
        assertThat(rec.sizeEstimateBytes).isEqualTo(500L);
    }

    @Test
    public void testNullNameObjectCreation() {
        /**
         * "The user should not be able to create an object with a null storagePlatform"
         */
        ObjectResource rec = fixture();
        rec.objectName = null;
        checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
    }

    @Test
    public void testNullStoragePlatformObjectCreation() {
        /**
         * "The user should not be able to create an object with a null storagePlatform"
         */
        ObjectResource rec = fixture();
        rec.storagePlatform = null;
        checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
    }

    @Test
    public void testBadStoragePlatformObjectCreation() {
        /**
         * "The user should not be able to create an object with a bogus storagePlatform"
         */
        ObjectResource rec = fixture();
        rec.storagePlatform = "xyzzy";
        checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
    }

    @Test
    public void testNoPathFilesystemObjectCreation() {
        /**
         * "The user should not be able to create a filesystem object without a directoryPath"
         */
        ObjectResource rec = fixture();
        rec.directoryPath = null;
        checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
    }

    @Test
    public void testNullOwnerObjectCreation() {
        /**
         * "The user should not be able to create an object with a null storagePlatform"
         */
        ObjectResource rec = fixture();
        rec.ownerId = null;
        checkStatus(BAD_REQUEST, post(new Client(), objectsPath(), rec));
    }

    @Test
    public void testBackdoorObjectCreation() {
        /**
         * "The user should not be able to create an object via an update"
         */
        ObjectResource rec = fixture();
        checkStatus(NOT_FOUND, post(new Client(), objectsPath()+"/xyzzy", rec));
    }

    @Test
    public void testFindByName() {
        Client client = new Client();
        ObjectResource rec = fixture();
        rec.objectName = UUID.randomUUID().toString();
        String queryURL = objectsPath()+"?name="+rec.objectName;

        // shouldn't be any objects with our unique name yet
        checkStatus(NOT_FOUND,get(client,queryURL,"me"));

        // make an object with our name, and see if we can find it
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        ClientResponse response = checkStatus(OK,get(client,queryURL,"me"));
        GenericType<List<ObjectResource>> genTyp = new GenericType<List<ObjectResource>>() {};
        List<ObjectResource> recs = response.getEntity(genTyp);
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
