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

import javax.ws.rs.core.MediaType;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;

public class ObjectResourceAcceptanceTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    @Test
    public void registerObjectTest() {
        Client client = new Client();

        GroupResource grp = new GroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[]{"tdanford"};
        grp.sizeEstimateBytes = 1000L;

        String groupPath = String.format("http://localhost:%d/group/store/objecttest1_group", RULE.getLocalPort());

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource obj = new ObjectResource();
        obj.name = "Test Object";
        obj.ownerId = "carlyeks";
        obj.readers = new String[]{"carlyeks", "tdanford"};
        obj.sizeEstimateBytes = 500L;

        String objectPath = String.format("%s/object1", groupPath);

        response = post(client, objectPath, obj);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectPath);
        assertThat(created.name).isEqualTo(obj.name);
        assertThat(created.ownerId).isEqualTo(obj.ownerId);
        assertThat(created.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(created.readers).isEqualTo(obj.readers);

    }

    @Test
    public void registerObjectAndDescribeTest() {
        Client client = new Client();

        GroupResource grp = new GroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[]{"tdanford"};
        grp.sizeEstimateBytes = 1000L;

        String groupPath = String.format("http://localhost:%d/group/store/objecttest2_group", RULE.getLocalPort());

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource obj = new ObjectResource();
        obj.name = "Test Described Object";
        obj.ownerId = "tdanford";
        obj.readers = new String[]{"carlyeks", "tdanford"};
        obj.sizeEstimateBytes = 500L;

        String objectPath = String.format("%s/object2", groupPath);

        response = post(client, objectPath, obj);

        assertThat(response.getStatus()).isEqualTo(200);

        response = get(client, objectPath);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectPath);
        assertThat(created.name).isEqualTo(obj.name);
        assertThat(created.ownerId).isEqualTo(obj.ownerId);
        assertThat(created.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(created.readers).isEqualTo(obj.readers);

    }

    @Test
    public void registerDescribeAndDeleteTest() {
        Client client = new Client();

        GroupResource grp = new GroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[]{"tdanford"};
        grp.sizeEstimateBytes = 1000L;

        String groupPath = String.format("http://localhost:%d/group/store/objecttest3_group", RULE.getLocalPort());

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource obj = new ObjectResource();
        obj.name = "Test Described Object";
        obj.ownerId = "tdanford";
        obj.readers = new String[]{"carlyeks", "tdanford"};
        obj.sizeEstimateBytes = 500L;

        String objectPath = String.format("%s/object_to_delete", groupPath);

        response = post(client, objectPath, obj);

        assertThat(response.getStatus()).isEqualTo(200);

        response = get(client, objectPath);

        assertThat(response.getStatus()).isEqualTo(200);

        ObjectResource created = response.getEntity(ObjectResource.class);

        assertThat(created.objectId).isEqualTo(objectPath);
        assertThat(created.name).isEqualTo(obj.name);
        assertThat(created.ownerId).isEqualTo(obj.ownerId);
        assertThat(created.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(created.readers).isEqualTo(obj.readers);

        response = delete(client, objectPath);

        assertThat(response.getStatus()).isEqualTo(200);

        response = get(client, objectPath);

        assertThat(response.getStatus()).isEqualTo(410);
    }
}
