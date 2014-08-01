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
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class FsGroupResourceAcceptanceTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));


    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    @Test
    public void registerFsGroupTest() {
        Client client = new Client();

        FsGroupResource grp = new FsGroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[] { "testuser" };
        grp.directory = "test_dir";

        String groupId = "foo";
        String groupPath = String.format("http://localhost:%d/group/fs/%s", RULE.getLocalPort(), groupId);

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        FsGroupResource respGrp = response.getEntity(FsGroupResource.class);

        assertThat(respGrp.groupId).isEqualTo(groupId);
        assertThat(respGrp.ownerId).isEqualTo(grp.ownerId);
        assertThat(respGrp.readers).isEqualTo(grp.readers);
        assertThat(respGrp.directory).isEqualTo(grp.directory);
    }

    @Test
    public void registerAndDescribeGroupTest() {
        Client client = new Client();

        FsGroupResource grp = new FsGroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[] { "testuser", "tdanford", "carlyeks" };
        grp.directory = "test_dir";

        String groupId = "testgroup1";
        String groupPath = String.format("http://localhost:%d/group/fs/%s", RULE.getLocalPort(), groupId);

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        response = get(client, groupPath);

        assertThat(response.getStatus()).isEqualTo(200);

        FsGroupResource described = response.getEntity(FsGroupResource.class);
        assertThat(described.groupId).isEqualTo(groupId);
        assertThat(described.ownerId).isEqualTo(grp.ownerId);
        assertThat(described.readers).isEqualTo(grp.readers);
        assertThat(described.directory).isEqualTo(grp.directory);
    }

    @Test
    public void registerAndForbiddenDescribeGroupTest() {
        Client client = new Client();

        FsGroupResource grp = new FsGroupResource();
        grp.ownerId = "tdanford";
        grp.readers = new String[] { "tdanford" };
        grp.writers = new String[] { "tdanford", "carlyeks" };
        grp.directory = "test_dir";

        String groupId = "forbiddengroup1";
        String groupPath = String.format("http://localhost:%d/group/fs/%s", RULE.getLocalPort(), groupId);

        ClientResponse response = post(client, groupPath, grp);

        assertThat(response.getStatus()).isEqualTo(200);

        // GETting the group as 'testuser' leads to a forbidden response
        response = get(client, groupPath, "testuser");
        assertThat(response.getStatus())
                .describedAs("response to describing the group by a user without read access")
                .isEqualTo(403);

        // But trying it with one of the Group's readers leads to the normal 200
        response = get(client, groupPath, "tdanford");
        assertThat(response.getStatus())
                .describedAs("response to describing the group by a user with read access")
                .isEqualTo(200);

        FsGroupResource described = response.getEntity(FsGroupResource.class);
        assertThat(described.groupId).isEqualTo(groupId);
        assertThat(described.ownerId).isEqualTo(grp.ownerId);
        assertThat(described.readers).isEqualTo(grp.readers);
        assertThat(described.writers).isEqualTo(grp.writers);
        assertThat(described.directory).isEqualTo(grp.directory);
    }

}
