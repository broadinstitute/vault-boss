package org.genomebridge.boss.http;

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;


/**
 * Created by davidan on 9/3/14.
 */
public class StoragePlatformTest extends ResourcedTest {

    @Test
    public void testEnumEquality() {
        assertThat( (StoragePlatform.OBJECTSTORE.getValue()).equals( "objectstore" )).isTrue();
        assertThat( (StoragePlatform.FILESYSTEM.getValue()).equals( "filesystem" )).isTrue();
    }

    @Test
    public void testObjectResourcePlatforms() {
        ObjectResource obj = new ObjectResource();
        obj.storagePlatform = "objectstore";

        assertThat(obj.isObjectStoreObject()).isTrue();
        assertThat(obj.isFilesystemObject()).isFalse();

        ObjectResource obj2 = new ObjectResource();
        obj2.storagePlatform = "filesystem";

        assertThat(obj2.isObjectStoreObject()).isFalse();
        assertThat(obj2.isFilesystemObject()).isTrue();

        ObjectResource obj3 = new ObjectResource();
        obj3.storagePlatform = "foobar";

        assertThat(obj3.isObjectStoreObject()).isFalse();
        assertThat(obj3.isFilesystemObject()).isFalse();

        ObjectResource obj4 = new ObjectResource();
        obj4.storagePlatform = null;

        assertThat(obj4.isObjectStoreObject()).isFalse();
        assertThat(obj4.isFilesystemObject()).isFalse();

    }

}
