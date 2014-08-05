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
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.util.Arrays;
import java.util.List;
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
        api = new DatabaseBossAPI(dao);
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
    public void testUpdateAndRetrieveGroup() {
        GroupResource group = new GroupResource();
        group.groupId = randomID();
        group.ownerId = "tdanford";
        group.sizeEstimateBytes = 1000L;
        group.typeHint = "typeHint";
        group.readers = new String[] { "tdanford", "testuser" };
        group.writers = new String[] { "carlyeks", "tdanford", "testuser" };

        api.updateGroup(group);

        GroupResource retrieved = api.getGroup(group.groupId);

        assertThat(retrieved.groupId).isEqualTo(group.groupId);
        assertThat(retrieved.ownerId).isEqualTo(group.ownerId);
        assertThat(retrieved.sizeEstimateBytes).isEqualTo(group.sizeEstimateBytes);
        assertThat(retrieved.typeHint).isEqualTo(group.typeHint);
        assertThat(retrieved.readers).containsOnly("tdanford", "testuser");
        assertThat(retrieved.writers).containsOnly("tdanford", "carlyeks", "testuser");
    }

    @Test
    public void testUpdateModifyAndRetrieveGroup() {
        GroupResource group = new GroupResource();
        group.groupId = randomID();
        group.ownerId = "tdanford";
        group.sizeEstimateBytes = 1000L;
        group.typeHint = "typeHint";
        group.readers = new String[] { "tdanford", "testuser" };
        group.writers = new String[] { "carlyeks", "tdanford", "testuser" };

        api.updateGroup(group);

        group.readers = new String[] { "tdanford" };
        group.writers = new String[] { "carlyeks", "foo" };

        api.updateGroup(group);

        GroupResource retrieved = api.getGroup(group.groupId);

        assertThat(retrieved.groupId).isEqualTo(group.groupId);
        assertThat(retrieved.ownerId).isEqualTo(group.ownerId);
        assertThat(retrieved.sizeEstimateBytes).isEqualTo(group.sizeEstimateBytes);
        assertThat(retrieved.typeHint).isEqualTo(group.typeHint);
        assertThat(retrieved.readers).containsOnly("tdanford");
        assertThat(retrieved.writers).containsOnly("carlyeks", "foo");
    }

    @Test
    public void testUpdateAndRetrieveObject() {
        ObjectResource obj = new ObjectResource();
        obj.objectId = randomID();
        obj.group = randomID();
        obj.ownerId = "tdanford";
        obj.sizeEstimateBytes = 1000L;
        obj.name = "Test Name";
        obj.readers = new String[] { "tdanford", "testuser" };
        obj.writers = new String[] { "carlyeks", "tdanford", "testuser" };

        api.updateObject(obj);

        ObjectResource retrieved = api.getObject(obj.objectId, obj.group);

        assertThat(retrieved.objectId).isEqualTo(obj.objectId);
        assertThat(retrieved.group).isEqualTo(obj.group);
        assertThat(retrieved.ownerId).isEqualTo(obj.ownerId);
        assertThat(retrieved.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(retrieved.name).isEqualTo(obj.name);
        assertThat(retrieved.readers).containsOnly("tdanford", "testuser");
        assertThat(retrieved.writers).containsOnly("tdanford", "carlyeks", "testuser");
    }



}
