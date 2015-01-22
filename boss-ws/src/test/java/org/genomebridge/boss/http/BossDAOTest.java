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

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.sql.Timestamp;
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
        try {
            return BossApplication.getDAO(RULE.getConfiguration(), RULE.getEnvironment());
        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private String createObject()
    {
        ObjectResource rec = fixture();
        rec.objectId = UUID.randomUUID().toString();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, rec.directoryPath, rec.storagePlatform, "me", now);
        return rec.objectId;
    }

    @Test
    public void testInsertAndListReaders() {
        String id = createObject();
        dao.insertReaders(id, Arrays.asList("tdanford", "carlyeks"));
        List<String> readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("tdanford", "carlyeks");
    }

    @Test
    public void testInsertAndDeleteReaders() {
        String id = createObject();

        dao.insertReaders(id, Arrays.asList("tdanford", "carlyeks"));

        List<String> readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("tdanford", "carlyeks");

        dao.deleteReaders(id, Arrays.asList("tdanford", "foo"));

        readers = dao.findReadersById(id);
        assertThat(readers).containsOnly("carlyeks");
    }

    @Test
    public void testInsertAndListWriters() {
        String id = createObject();
        dao.insertWriters(id, Arrays.asList("tdanford", "carlyeks"));
        List<String> writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford", "carlyeks");
    }

    @Test
    public void testInsertAndDeleteWriters() {
        String id = createObject();

        dao.insertWriters(id, Arrays.asList("tdanford", "carlyeks"));

        List<String> writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford", "carlyeks");

        dao.deleteWriters(id, Arrays.asList("carlyeks", "foo"));

        writers = dao.findWritersById(id);
        assertThat(writers).containsOnly("tdanford");
    }

    @Test
    public void testInsertAndGetObject() {
        ObjectResource rec = new ObjectResource();
        rec.objectId = UUID.randomUUID().toString();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.objectName = "Name";
        rec.storagePlatform = StoragePlatform.FILESYSTEM.getValue();
        rec.directoryPath = "/some/path";

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes,
                            rec.directoryPath, rec.storagePlatform, "remoteUser", now);

        ObjectResource fetched = dao.findObjectById(rec.objectId);

        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.objectName).isEqualTo(rec.objectName);
        assertThat(fetched.storagePlatform).isEqualTo(rec.storagePlatform);
    }

    @Test
    public void testInsertAndUpdateObject() {
        ObjectResource rec = new ObjectResource();
        rec.objectId = UUID.randomUUID().toString();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.objectName = "Name";
        rec.storagePlatform = StoragePlatform.FILESYSTEM.getValue();
        rec.directoryPath = "/some/path";

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes,
                            rec.directoryPath, rec.storagePlatform, "remoteUser", now);

        ObjectResource fetched = dao.findObjectById(rec.objectId);

        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.objectName).isEqualTo(rec.objectName);
        assertThat(fetched.storagePlatform).isEqualTo(rec.storagePlatform);

        rec.objectName = rec.objectName + "xyzzy";
        rec.ownerId = "carlyeks";

        now = new Timestamp(System.currentTimeMillis());
        dao.updateObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, now);

        fetched = dao.findObjectById(rec.objectId);

        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.objectId).isEqualTo(rec.objectId);
        assertThat(fetched.ownerId).isEqualTo(rec.ownerId);
        assertThat(fetched.sizeEstimateBytes).isEqualTo(rec.sizeEstimateBytes);
        assertThat(fetched.objectName).isEqualTo(rec.objectName);
        assertThat(fetched.storagePlatform).isEqualTo(rec.storagePlatform);
    }

    @Test
    public void testTimestamps() {
        ObjectResource rec = new ObjectResource();
        rec.objectId = UUID.randomUUID().toString();
        rec.ownerId = "tdanford";
        rec.sizeEstimateBytes = 1000L;
        rec.objectName = "Name";
        rec.storagePlatform = StoragePlatform.FILESYSTEM.getValue();
        rec.directoryPath = "/some/path";

        Timestamp cDate = new Timestamp(System.currentTimeMillis());
        dao.insertObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes,
                            rec.directoryPath, rec.storagePlatform, "remoteUser", cDate);
        ObjectResource fetched = dao.findObjectById(rec.objectId);
        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.createdBy).isEqualTo("remoteUser");
        assertThat(fetched.createDate).isEqualTo(cDate);
        assertThat(fetched.modifyDate).isNull();
        assertThat(fetched.resolveDate).isNull();
        assertThat(fetched.deleteDate).isNull();

        Timestamp mDate = new Timestamp(System.currentTimeMillis());
        dao.updateObject(rec.objectId, rec.objectName, rec.ownerId, rec.sizeEstimateBytes, mDate);
        fetched = dao.findObjectById(rec.objectId);
        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.createdBy).isEqualTo("remoteUser");
        assertThat(fetched.createDate).isEqualTo(cDate);
        assertThat(fetched.modifyDate).isEqualTo(mDate);
        assertThat(fetched.resolveDate).isNull();
        assertThat(fetched.deleteDate).isNull();

        Timestamp rDate = new Timestamp(System.currentTimeMillis());
        dao.updateResolveDate(rec.objectId, rDate);
        fetched = dao.findObjectById(rec.objectId);
        assertThat(fetched.active).isEqualTo("Y");
        assertThat(fetched.createdBy).isEqualTo("remoteUser");
        assertThat(fetched.createDate).isEqualTo(cDate);
        assertThat(fetched.modifyDate).isEqualTo(mDate);
        assertThat(fetched.resolveDate).isEqualTo(rDate);
        assertThat(fetched.deleteDate).isNull();

        Timestamp dDate = new Timestamp(System.currentTimeMillis());
        dao.deleteObject(rec.objectId, dDate);
        fetched = dao.findObjectById(rec.objectId);
        assertThat(fetched.active).isEqualTo("N");
        assertThat(fetched.createdBy).isEqualTo("remoteUser");
        assertThat(fetched.createDate).isEqualTo(cDate);
        assertThat(fetched.modifyDate).isEqualTo(mDate);
        assertThat(fetched.resolveDate).isEqualTo(rDate);
        assertThat(fetched.deleteDate).isEqualTo(dDate);
    }
}
