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
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class GroupResourceAcceptanceTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));


    @Test
    public void registerGroupTest() {
        Client client = new Client();

        GroupResource grp = new GroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[] { "tdanford" };
        grp.sizeEstimateBytes = 1000L;

        String groupPath = String.format("http://localhost:%d/group/foo", RULE.getLocalPort());

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        GroupResource respGrp = response.getEntity(GroupResource.class);

        assertThat(respGrp.groupId).isEqualTo(groupPath);
        assertThat(respGrp.ownerId).isEqualTo(grp.ownerId);
        assertThat(respGrp.readers).isEqualTo(grp.readers);
        assertThat(respGrp.sizeEstimateBytes).isEqualTo(grp.sizeEstimateBytes);
    }

    @Test
    public void registerAndDescribeGroupTest() {
        Client client = new Client();

        GroupResource grp = new GroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[] { "tdanford", "carlyeks" };
        grp.sizeEstimateBytes = 1000L;

        String groupPath = String.format("http://localhost:%d/group/testgroup1", RULE.getLocalPort());

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        response = get(client, groupPath);

        assertThat(response.getStatus()).isEqualTo(200);

        GroupResource described = response.getEntity(GroupResource.class);
        assertThat(described.groupId).isEqualTo(groupPath);
        assertThat(described.ownerId).isEqualTo(grp.ownerId);
        assertThat(described.readers).isEqualTo(grp.readers);
        assertThat(described.sizeEstimateBytes).isEqualTo(grp.sizeEstimateBytes);
    }
}
