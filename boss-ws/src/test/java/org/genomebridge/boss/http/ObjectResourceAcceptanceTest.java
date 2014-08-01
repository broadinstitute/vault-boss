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
import org.genomebridge.boss.http.resources.*;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.fest.assertions.api.Assertions.assertThat;

public class ObjectResourceAcceptanceTest extends AbstractTest {

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
        /**
         * "The user should be able to register new objects to a group."
         */

        String groupId = "objecttest1_group";
        String objectId = "object1";

        check200( createGroup(groupId, "tdanford", "test_hint", 1010L) );

        String name = "Test Object";
        String owner = "carlyeks";
        Long sizeEstimate = 1010L;

        ClientResponse response = createObject(objectId, groupId, name, owner, sizeEstimate);

        check200( response );

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectId);
        assertThat(created.name).isEqualTo(name);
        assertThat(created.ownerId).isEqualTo(owner);
        assertThat(created.sizeEstimateBytes).isEqualTo(sizeEstimate);
        assertThat(created.readers).containsOnly("carlyeks", "testuser");
        assertThat(created.writers).containsOnly("carlyeks", "testuser");
    }

    @Test
    public void registerObjectAndDescribeTest() {
        Client client = new Client();

        String groupId = "objecttest2_group";
        String objectId = "object2";

        check200( createGroup(groupId, "tdanford", "test_hint", 1020L) );
        check200( createObject(objectId, groupId, "Test Name", "tdanford", 1010L) );

        ClientResponse response = get(client, objectPath(objectId, groupId));

        check200(response);

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created.objectId).describedAs("created object id").isEqualTo(objectId);
        assertThat(created.name).describedAs("created object name").isEqualTo("Test Name");
        assertThat(created.sizeEstimateBytes).isEqualTo(1010L);
        assertThat(created.writers).describedAs("created object writers").containsOnly("tdanford", "testuser");
        assertThat(created.readers).describedAs("created object readers").containsOnly("tdanford", "testuser");
    }

    @Test
    public void registerDescribeAndDeleteTest() {
        Client client = new Client();

        String groupId = "group_with_deletable_objects_1";
        String objectId = "deletable_1";

        check200( createGroup(groupId, "tdanford", "test_hint", 1020L) );
        check200( createObject(objectId, groupId, "Test Name", "tdanford", 1010L) );
        check200( get(client, objectPath(objectId, groupId)) );
        check200(delete(client, objectPath(objectId, groupId)));
        checkStatus( 410, get(client, objectPath(objectId, groupId)) );
    }

    @Test
    public void setIllegalDeleteOnObject() {
        Client client = new Client();
        String groupId = randomID(), objectId = randomID();

        // Create the group and the object...
        check200( createGroup(groupId, "tdanford", "test_hint", 100L) );
        check200( createObject(objectId, groupId, "Deletable", "tdanford", 100L ));

        // trying a DELETE with a username 'fake_user' should fail
        checkStatus(Response.Status.FORBIDDEN.getStatusCode(),
                delete(client, objectPath(objectId, groupId), "fake_user"));

        // object should still be there
        check200( get(client, objectPath(objectId, groupId)) );
    }

    @Test
    public void testSetPermissionsOnObject() {
        Client client = new Client();
        String groupId = randomID(), objectId = randomID();

        check200( createGroup(groupId, "tdanford", "test_hint", 100L));

        ClientResponse response = check200( createObject(objectId, groupId, "changeable", "tdanford", 100L));

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.readers = arrayAppend( rec.readers, "new_reader" );
        rec.writers = arrayAppend( rec.writers, "new_writer" );

        check200( post(client, objectPath(objectId, groupId), rec));

        response = check200( get(client, objectPath(objectId, groupId)));

        rec = response.getEntity(ObjectResource.class);

        assertThat(rec.readers).containsOnly("testuser", "tdanford", "new_reader");
        assertThat(rec.writers).containsOnly("testuser", "tdanford", "new_writer");
    }


    @Test
    public void setOwnerOnObject() {
        Client client = new Client();
        String groupId = randomID(), objectId = randomID();

        check200( createGroup(groupId, "tdanford", "test_hint", 100L));

        ClientResponse response = check200( createObject(objectId, groupId, "changeable", "tdanford", 100L));

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.ownerId = "new_owner";

        check200( post(client, objectPath(objectId, groupId), rec));

        response = check200( get(client, objectPath(objectId, groupId)));

        rec = response.getEntity(ObjectResource.class);

        assertThat(rec.ownerId).isEqualTo("new_owner");
    }

    @Test
    public void setNotAllowedToSetNameOnObject() {
        /**
         * "It's illegal for a user to set the Name field on an already-registered object."
         */
        Client client = new Client();
        String groupId = randomID(), objectId = randomID();

        check200( createGroup(groupId, "tdanford", "test_hint", 100L));

        ClientResponse response = check200( createObject(objectId, groupId, "changeable", "tdanford", 100L));

        ObjectResource rec = response.getEntity(ObjectResource.class);
        assertThat(rec).isNotNull();

        rec.name = "New Name";

        // It's illegal to change the name!
        checkStatus(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                post(client, objectPath(objectId, groupId), rec));

        response = check200( get(client, objectPath(objectId, groupId)));

        rec = response.getEntity(ObjectResource.class);

        // The owner is unchanged.
        assertThat(rec.ownerId).isEqualTo("tdanford");
    }

    @Test
    public void testIllegalSetPermissions() {
        Client client = new Client();

        String groupId = randomID(), objectId = randomID();

        check200( createGroup(groupId, "tdanford", "test_hint", 100L));
        ClientResponse response = check200( createObject(objectId, groupId, "test object", "tdanford", 100L));

        ObjectResource rec = response.getEntity(ObjectResource.class);

        rec.readers = arrayAppend(rec.readers, "new_reader");

        // It's illegal, as the user 'fake_user', to update the readers field of the ObjectResource
        checkStatus(403, post(client, objectPath(objectId, groupId), "fake_user", rec));

        response = check200( get(client, objectPath(objectId, groupId)) );

        rec = response.getEntity(ObjectResource.class);

        // The readers are unchanged.
        assertThat(rec.readers).containsOnly("testuser", "tdanford");
    }
}
