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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

public class BossDAOTest extends ResourcedTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    private static BossDAO dao = null;

    @BeforeClass
    public static void setup() {
       dao = dao();
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
    public void testInsertAndListReaders() {
        String id = randomID();
        dao.insertReaders(id, Arrays.asList("tdanford", "carlyeks"));
        List<String> readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("tdanford", "carlyeks");
    }

    @Test
    public void testInsertAndDeleteReaders() {
        String id = randomID();

        dao.insertReaders(id, Arrays.asList("tdanford", "carlyeks"));

        List<String> readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("tdanford", "carlyeks");

        dao.deleteReaders(id, Arrays.asList("tdanford", "foo"));

        readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("carlyeks");
    }

    @Test
    public void testInsertAndListWriters() {
        String id = randomID();
        dao.insertWriters(id, Arrays.asList("tdanford", "carlyeks"));
        List<String> writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford", "carlyeks");
    }

    @Test
    public void testInsertAndDeleteWriters() {
        String id = randomID();

        dao.insertWriters(id, Arrays.asList("tdanford", "carlyeks"));

        List<String> writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford", "carlyeks");

        dao.deleteWriters(id, Arrays.asList("carlyeks", "foo"));

        writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford");
    }

    @Test
    public void testInsertAndGetGroup() {
        GroupResource rec = new GroupResource();
        rec.groupId = randomID();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.typeHint = "type";

        dao.insertGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint, null);

        GroupResource fetched = dao.findGroupById(rec.groupId);

        assertThat(fetched.groupId).isEqualTo(rec.groupId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.typeHint).isEqualTo(rec.typeHint);
    }

    @Test
    public void testInsertAndUpdateGroup() {
        GroupResource rec = new GroupResource();
        rec.groupId = randomID();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.typeHint = "type";

        dao.insertGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint, null);

        GroupResource fetched = dao.findGroupById(rec.groupId);

        assertThat(fetched.groupId).isEqualTo(rec.groupId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.typeHint).isEqualTo(rec.typeHint);

        rec.ownerId = "carlyeks";
        rec.typeHint = rec.typeHint + randomID();
        rec.sizeEstimateBytes += 100L;

        dao.updateGroup(rec.groupId, rec.ownerId, rec.sizeEstimateBytes, rec.typeHint);

        fetched = dao.findGroupById(rec.groupId);

        assertThat(fetched.groupId).isEqualTo(rec.groupId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.typeHint).isEqualTo(rec.typeHint);
    }

    @Test
    public void testInsertAndGetObject() {
        ObjectResource rec = new ObjectResource();
        rec.objectId = randomID();
        rec.group = randomID();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.name = "Name";

        dao.insertObject(rec.objectId, rec.group, rec.ownerId, rec.sizeEstimateBytes, rec.name, null);

        ObjectResource fetched = dao.findObjectById(rec.objectId, rec.group);

        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.group).isEqualTo(rec.group);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.name).isEqualTo(rec.name);
    }

    @Test
    public void testInsertAndUpdateObject() {
        ObjectResource rec = new ObjectResource();
        rec.objectId = randomID();
        rec.group = randomID();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.name = "Name";

        dao.insertObject(rec.objectId, rec.group, rec.ownerId, rec.sizeEstimateBytes, rec.name, null);

        ObjectResource fetched = dao.findObjectById(rec.objectId, rec.group);

        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.group).isEqualTo(rec.group);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.name).isEqualTo(rec.name);

        rec.name = rec.name + randomID();
        rec.ownerId = "carlyeks";

        dao.updateObject(rec.objectId, rec.group, rec.ownerId, rec.sizeEstimateBytes, rec.name);

        fetched = dao.findObjectById(rec.objectId, rec.group);

        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.group).isEqualTo(rec.group);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.name).isEqualTo(rec.name);
    }

}
