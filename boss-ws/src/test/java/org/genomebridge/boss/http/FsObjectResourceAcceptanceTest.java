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
import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.TreeSet;

import static org.fest.assertions.api.Assertions.assertThat;

public class FsObjectResourceAcceptanceTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    public DropwizardAppRule<BossConfiguration> rule() { return RULE; }

    @Test
    public void registerObjectTest() {
        /**
         * Step 1. Register a group (and check the response is 200)
         * Step 2. Register an object (and check the response is 200)
         */

        String groupId = "objecttest1_group";
        String owner = "tdanford";
        String directory = "test_dir";

        ClientResponse response = createFsGroup(groupId, owner, directory);

        assertThat(response.getStatus()).isEqualTo(200);

        String objectId = "object1";
        String name = "Test Object";
        Long sizeEstimate = 500L;

        response = createFsObject(objectId, groupId, name, owner, sizeEstimate);

        assertThat(response.getStatus())
                .describedAs("response to registering the object")
                .isEqualTo(200);

        FsObjectResource created = response.getEntity(FsObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectId);
        assertThat(created.name).isEqualTo(name);
        assertThat(created.ownerId).isEqualTo(owner);
        assertThat(created.sizeEstimateBytes).isEqualTo(sizeEstimate);
        assertThat(created.readers).contains(owner);
        assertThat(created.readers).contains("testuser");
    }

    @Test
    public void registerObjectAndDescribeTest() {
        /**
         * 1. Create a group -- check that it returns 200
         * 2. Register an object -- check that it returns 200
         * 3. GET the object -- check that it returns 200, and an object with values identical
         *    to those that were registered.
         */
        Client client = new Client();

        // Step 1
        String groupId = "objecttest2_group";
        String owner = "tdanford";
        String directory = "test_dir";

        ClientResponse response = createFsGroup(groupId, owner, directory);

        assertThat(response.getStatus()).isEqualTo(200);

        // Step 2

        String objectId = "object2";
        String name = "Test Described Object";
        Long sizeEstimate = 500L;

        response = createFsObject(objectId, groupId, name, owner, sizeEstimate);

        assertThat(response.getStatus()).isEqualTo(200);

        // Step 3
        response = get(client, fsObjectPath(objectId, groupId));

        assertThat(response.getStatus()).isEqualTo(200);

        FsObjectResource created = response.getEntity(FsObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectId);
        assertThat(created.name).isEqualTo(name);
        assertThat(created.ownerId).isEqualTo(owner);
        assertThat(created.sizeEstimateBytes).isEqualTo(sizeEstimate);
        assertThat(created.readers).contains(owner);
        assertThat(created.readers).contains("testuser");

    }

    @Test
    public void registerDescribeAndDeleteTest() {
        /**
         * 1. Create a group
         * 2. Register an object
         * 3. Get the object and show that it returns 200 with the right fields
         * 4. Delete the object (and show that the delete call returns 200)
         * 5. Get the object again -- show that it returns 410 (Gone)
         */
        Client client = new Client();

        // Step 1
        String groupId = "objecttest3_group";

        ClientResponse response = createFsGroup(groupId, "tdanford", "test_dir");
        assertThat(response.getStatus()).isEqualTo(200);

        // Step 2
        String objectId = "object_to_delete";
        String name = "Test Object";
        String owner = "tdanford";
        Long sizeEstimate = 500L;

        response = createFsObject(objectId, groupId, name, owner, sizeEstimate );
        assertThat(response.getStatus()).isEqualTo(200);

        // Step 3
        response = get(client, fsObjectPath(objectId, groupId));

        assertThat(response.getStatus()).isEqualTo(200);

        FsObjectResource created = response.getEntity(FsObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectId);
        assertThat(created.name).isEqualTo(name);
        assertThat(created.ownerId).isEqualTo(owner);
        assertThat(created.sizeEstimateBytes).isEqualTo(sizeEstimate);
        assertThat(created.readers).contains(owner);
        assertThat(created.readers).contains("testuser");
        assertThat(created.writers).contains(owner);
        assertThat(created.writers).contains("testuser");

        // Step 4
        response = delete(client, fsObjectPath(objectId, groupId));

        assertThat(response.getStatus()).isEqualTo(200);

        // Step 5
        response = get(client, fsObjectPath(objectId, groupId));

        assertThat(response.getStatus()).isEqualTo(410);
    }
}
