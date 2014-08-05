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
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

public class GroupResourceAcceptanceTest extends AbstractTest {

    public static int FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();
    public static int CREATED = Response.Status.CREATED.getStatusCode();

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    @Test
    public void registerGroupTest() {
        String groupId = randomID();
        String owner = "tdanford";
        String typeHint = "test_type_hint";
        Long sizeEstimate = 500L;

        ClientResponse response = check200(createGroup(groupId, owner, typeHint, sizeEstimate));

        GroupResource respGrp = response.getEntity(GroupResource.class);

        assertThat(respGrp.groupId).isEqualTo(groupId);
        assertThat(respGrp.ownerId).isEqualTo(owner);
        assertThat(respGrp.readers).containsOnly(owner, "testuser");
        assertThat(respGrp.writers).containsOnly(owner, "testuser");
        assertThat(respGrp.typeHint).isEqualTo(typeHint);
        assertThat(respGrp.sizeEstimateBytes).isEqualTo(sizeEstimate);
    }

    @Test
    public void registerAndDescribeGroupTest() {
        /**
         * Step 1: Create a group, and check that the response is 200
         * Step 2: GET the group, and make sure the fields match the original inputs.
         */
        Client client = new Client();

        // Step 1
        String groupId = "testgroup1";
        String owner = "tdanford";
        String typeHint = "test_type_hint";
        Long sizeEstimate = 1000L;

        ClientResponse response = createGroup(groupId, owner, typeHint, sizeEstimate);

        assertThat(response.getStatus()).isEqualTo(200);

        // Step 2
        response = get(client, groupPath(groupId), "testuser");

        assertThat(response.getStatus()).isEqualTo(200);

        GroupResource described = response.getEntity(GroupResource.class);
        assertThat(described.groupId).isEqualTo(groupId);
        assertThat(described.ownerId).isEqualTo(owner);
        assertThat(described.readers).contains("testuser");
        assertThat(described.writers).contains(owner);
        assertThat(described.writers).contains("testuser");
        assertThat(described.typeHint).isEqualTo(typeHint);
        assertThat(described.sizeEstimateBytes).isEqualTo(sizeEstimate);
    }

    @Test
    public void testDescribeAuthOnGroup() {
        /**
         * Step 1: Create a group, and check that the response is 200
         * Step 2: GET the group as a user not in the readers list,
         *         and make sure the response is a 403
         * Step 3: GET the group as a user in teh readers list, and make sure the response is a 200
         *         and that the fields are correctly populated
         */
        Client client = new Client();

        // Step 1
        String groupId = "testgroup1";
        String owner = "tdanford";
        String typeHint = "test_type_hint";
        Long sizeEstimate = 1000L;

        ClientResponse response = createGroup(groupId, owner, typeHint, sizeEstimate);

        assertThat(response.getStatus()).isEqualTo(200);

        // Step 2
        response = get(client, groupPath(groupId), "illegal_user");

        assertThat(response.getStatus())
                .describedAs("response code of illegal access")
                .isEqualTo(403);

        // Step 3
        response = get(client, groupPath(groupId), "testuser");

        assertThat(response.getStatus()).isEqualTo(200);

        GroupResource described = response.getEntity(GroupResource.class);
        assertThat(described.groupId).isEqualTo(groupId);
        assertThat(described.ownerId).isEqualTo(owner);
        assertThat(described.readers).contains("testuser");
        assertThat(described.writers).contains(owner);
        assertThat(described.writers).contains("testuser");
        assertThat(described.typeHint).isEqualTo(typeHint);
        assertThat(described.sizeEstimateBytes).isEqualTo(sizeEstimate);
    }

    @Test
    public void testSetOwnerOnGroup() {
        /**
         * Step 1: Create a group
         * Step 2: GET the group and verify that its owner field is at its original value.
         * Step 3: POST to the group with a modified owner field, and verify that the POST is
         *         accepted.
         * Step 4: GET the group again, and verify that its owner field has been updated.
         */

        Client client = new Client();

        // Step 1
        String groupId = "permissions_group3";

        ClientResponse response = createGroup(groupId, "tdanford", "typeHint", 500L);

        assertThat(response.getStatus())
                .describedAs("Response to registering the group")
                .isEqualTo(200);


        // Step 2
        response = get(client, groupPath(groupId));

        assertThat(response.getStatus())
                .describedAs("Response to GETting the registered group")
                .isEqualTo(200);

        GroupResource posted = response.getEntity(GroupResource.class);

        assertThat(posted.ownerId)
                .describedAs("readers field of the registered object")
                .isEqualTo("tdanford");

        // Step 3
        posted.ownerId = "testuser";

        response = post(client, groupPath(groupId), posted);

        assertThat(response.getStatus())
                .describedAs("response to updating the group")
                .isEqualTo(200);

        // Step 4
        response = get(client, groupPath(groupId));

        assertThat(response.getStatus())
                .describedAs("response to getting the resource a second time")
                .isEqualTo(200);

        posted = response.getEntity(GroupResource.class);

        assertThat(posted).isNotNull();
        assertThat(posted.ownerId)
                .describedAs("ownerId field of the updated resource")
                .isEqualTo("testuser");
    }

    @Test
    public void testSetPermissionsOnGroup() {
        /**
         * Step 1: Create a group
         * Step 2: GET the group and verify that its readers list and writers list are at their original settings.
         * Step 3: POST to the group with a modified readers/writers field, and verify that the POST is
         *         accepted.
         * Step 4: GET the group again, and verify that its readers/writers fields have been updated.
         */

        Client client = new Client();

        // Step 1
        String groupId = "permissions_group1";

        ClientResponse response = createGroup(groupId, "tdanford", "typeHint", 500L);

        assertThat(response.getStatus())
                .describedAs("Response to registering the group")
                .isEqualTo(200);


        // Step 2
        response = get(client, groupPath(groupId));

        assertThat(response.getStatus())
                .describedAs("Response to GETting the registered group")
                .isEqualTo(200);

        GroupResource posted = response.getEntity(GroupResource.class);

        assertThat(posted.readers)
                .describedAs("readers field of the registered object")
                .containsOnly("testuser", "tdanford");

        // Step 3
        posted.readers = arrayAppend( posted.readers, "new_reader" );

        response = post(client, groupPath(groupId), posted);

        assertThat(response.getStatus())
                .describedAs("response to updating the group")
                .isEqualTo(200);

        // Step 4
        response = get(client, groupPath(groupId));

        assertThat(response.getStatus())
                .describedAs("response to getting the resource a second time")
                .isEqualTo(200);

        posted = response.getEntity(GroupResource.class);

        assertThat(posted).isNotNull();
        assertThat(posted.readers)
                .describedAs("readers field of the updated resource")
                .containsOnly("testuser", "tdanford", "new_reader");
    }

    @Test
    public void testIllegalSetPermissionsOnGroup() {
        /**
         * Step 1: Create a group
         * Step 2: POST to the group with a modified readers/writers field but as a user who is *not* in the
         *         writers field, and verify that we receive a 403 Forbidden response.
         */

        Client client = new Client();

        // Step 1
        String groupId = "permissions_group2";

        ClientResponse response = createGroup(groupId, "tdanford", "typeHint", 500L);

        assertThat(response.getStatus())
                .describedAs("Response to registering the group")
                .isEqualTo(200);

        GroupResource posted = response.getEntity(GroupResource.class);

        // Step 2
        posted.readers = arrayAppend( posted.readers, "new_reader" );

        response = post(client, groupPath(groupId), "new_reader", posted);

        assertThat(response.getStatus())
                .describedAs("response to illegal setting of the permissions")
                .isEqualTo(403);
    }

    @Test
    public void testIllegalSetOwnerOnGroup() {
        /**
         * Step 1: Create a group
         * Step 2: POST to the group with a modified owner field but as a user who is *not* in the
         *         writers field, and verify that we receive a 403 Forbidden response.
         * Step 3: Check that a GET on the group returns the original owner field.
         */

        Client client = new Client();

        // Step 1
        String groupId = "permissions_group4";

        ClientResponse response = check200(createGroup(groupId, "tdanford", "typeHint", 500L));

        GroupResource posted = response.getEntity(GroupResource.class);

        // Step 2
        posted.ownerId = "new_owner";

        checkStatus(FORBIDDEN, post(client, groupPath(groupId), "new_reader", posted));

        // Step 3
        response = check200( get(client, groupPath(groupId) ));

        posted = response.getEntity(GroupResource.class);

        assertThat(posted).isNotNull();
        assertThat(posted.ownerId).isEqualTo("tdanford");
    }


    @Test
    public void testAnonymousObjectCreation() {
        /**
         * "Users should be able to create a new Object without an ID in hand, by
         * POSTing to /group/store/{groupId}/objects"
         */

        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createAnonymousGroup("tdanford", "typeHint", 1000L));

        String location = checkHeader(response, "Location");

        response = check200( get(client, location ));

        GroupResource group = response.getEntity(GroupResource.class);

        response = checkStatus(CREATED, createAnonymousObject(group.groupId, "Test Name", "tdanford", 500L));

        String objectLocation = checkHeader(response, "Location");

        response = check200( get(client, objectLocation ));

        ObjectResource obj = response.getEntity(ObjectResource.class);

        assertThat(obj).isNotNull();
        assertThat(obj.ownerId).isEqualTo("tdanford");
        assertThat(obj.name).isEqualTo("Test Name");
        assertThat(obj.sizeEstimateBytes).isEqualTo(500L);
    }
}
