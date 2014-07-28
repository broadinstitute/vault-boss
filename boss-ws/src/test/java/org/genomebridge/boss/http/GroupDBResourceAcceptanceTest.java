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
import org.junit.*;

import static org.fest.assertions.api.Assertions.assertThat;

public class GroupDBResourceAcceptanceTest extends AbstractTest {

    public static int CREATED = ClientResponse.Status.CREATED.getStatusCode();

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

        GroupResource grp = new GroupResource();
        grp.groupId = "foo";
        grp.ownerId = "tdanford";
        grp.sizeEstimateBytes = 1000L;
        grp.typeHint = "typeHint";
        grp.readers = new String[] { "testuser" };
        grp.writers = new String[] { "testuser" };

        String path = String.format("http://localhost:%d/db/group", RULE.getLocalPort());

        ClientResponse response = post(client, path, grp);

        checkStatus( CREATED, response );

        String location = checkHeader(response, "Location");

        response = check200( get(client, location) );

        GroupResource respGrp = response.getEntity(GroupResource.class);

        assertThat(respGrp.groupId).isEqualTo(grp.groupId);
        assertThat(respGrp.ownerId).isEqualTo(grp.ownerId);
        assertThat(respGrp.sizeEstimateBytes).isEqualTo(grp.sizeEstimateBytes);
        assertThat(respGrp.typeHint).isEqualTo(grp.typeHint);
    }

}
