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
import org.genomebridge.boss.http.resources.GroupResource;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URI;

import static org.fest.assertions.api.Assertions.assertThat;

public class AllGroupsAcceptanceTest extends AbstractTest {

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
    public void testObjectStoreGroupCreation() {
        /**
         * "The user should be able to create a new GroupResource by POSTing to
         * groups/store"
         */

        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createAnonymousGroup("tdanford", "typeHint", 500L));

        String location = checkHeader(response, "Location");

        response = check200( get(client, location) );

        GroupResource rec = response.getEntity(GroupResource.class);

        assertThat(rec).isNotNull();
        assertThat(rec.ownerId).isEqualTo("tdanford");
        assertThat(rec.typeHint).isEqualTo("typeHint");
        assertThat(rec.sizeEstimateBytes).isEqualTo(500L);
    }

    @Test
    public void testFilesystemGroupCreation() {
        /**
         * "The user should be able to create a new FsGroupResource by POSTing to
         * groups/fs"
         */

        Client client = new Client();

        ClientResponse response = checkStatus(CREATED, createAnonymousFsGroup("tdanford", "test_dir"));

        String location = checkHeader(response, "Location");

        response = check200( get(client, location) );

        FsGroupResource rec = response.getEntity(FsGroupResource.class);

        assertThat(rec).isNotNull();
        assertThat(rec.ownerId).isEqualTo("tdanford");
        assertThat(rec.directory).isEqualTo("test_dir");
    }
}
