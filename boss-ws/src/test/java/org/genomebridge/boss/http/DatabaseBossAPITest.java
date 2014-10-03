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

import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.HttpMethod;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.genomebridge.boss.http.objectstore.S3ObjectStore;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.net.URI;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

public class DatabaseBossAPITest extends ResourcedTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    private static BossDAO dao = null;
    private static BossAPI api = null;

    @BeforeClass
    public static void setup() {
        dao = dao();
        ObjectStore objectStore = new S3ObjectStore(
                RULE.getConfiguration().getObjectStoreConfiguration().createClient(),
                RULE.getConfiguration().getObjectStoreConfiguration().getBucket());
        api = new DatabaseBossAPI(dao, objectStore);
    }

    private static BossDAO dao() {
        BossConfiguration config = RULE.getConfiguration();
        Environment env = RULE.getEnvironment();
        final DBIFactory factory = new DBIFactory();
        try {
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            return jdbi.onDemand(BossDAO.class);

        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private String randomID() { return UUID.randomUUID().toString(); }

    @Test
    public void testUpdateAndRetrieveObject() {
        ObjectResource obj = new ObjectResource();
        obj.objectId = randomID();
        obj.ownerId = "tdanford";
        obj.sizeEstimateBytes = 1000L;
        obj.objectName = "Test Name";
        obj.readers = new String[] { "tdanford", "testuser" };
        obj.writers = new String[] { "carlyeks", "tdanford", "testuser" };
        obj.storagePlatform = "platform";

        api.updateObject(obj.objectId, obj);

        ObjectResource retrieved = api.getObject(obj.objectId);

        assertThat(retrieved.objectId).isEqualTo(obj.objectId);
        assertThat(retrieved.ownerId).isEqualTo(obj.ownerId);
        assertThat(retrieved.objectName).isEqualTo(obj.objectName);
        assertThat(retrieved.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(retrieved.storagePlatform).isEqualTo(obj.storagePlatform);
        assertThat(retrieved.readers).containsOnly("tdanford", "testuser");
        assertThat(retrieved.writers).containsOnly("tdanford", "carlyeks", "testuser");
    }


    @Test
    public void testGeneratePresignedURL() {

        ObjectResource obj = new ObjectResource();
        obj.objectId = randomID();
        obj.ownerId = "tdanford";
        obj.sizeEstimateBytes = 1000L;
        obj.objectName = "Test Name";
        obj.readers = new String[] { "tdanford", "testuser" };
        obj.writers = new String[] { "carlyeks", "tdanford", "testuser" };
        obj.storagePlatform = "objectstore";

        api.updateObject(obj.objectId, obj);

        URI uri = api.getPresignedURL(obj.objectId, HttpMethod.GET, 10*1000);

        assertThat(uri).isNotNull();
        assertThat(uri.getHost()).isEqualTo("genomebridge-variantstore-ci.s3.amazonaws.com");
        assertThat(uri.toString()).startsWith(
                String.format("https://genomebridge-variantstore-ci.s3.amazonaws.com/%s-",
                        obj.objectId));
    }

}
