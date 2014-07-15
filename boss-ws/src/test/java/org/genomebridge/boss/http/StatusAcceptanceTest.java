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
import org.junit.*;

import java.net.URL;

import static org.fest.assertions.api.Assertions.*;

public class StatusAcceptanceTest {

    public static String resourceFilePath(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resource = loader.getResource(name);
        return resource != null ? resource.getFile() : null;
    }

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    @Test
    public void loginHandlerRedirectsAfterPost() {
        Client client = new Client();

        ClientResponse response = client.resource(
                String.format("http://localhost:%d/ok", RULE.getLocalPort()))
                .get(ClientResponse.class);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
