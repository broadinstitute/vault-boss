package org.genomebridge.boss.http;

import java.net.URL;
import java.util.Arrays;
import java.util.TreeSet;

import org.genomebridge.boss.http.models.ObjectDesc;

/**
 * Abstract super-class for tests, that allows easy loading of resources from the classpath.
 */
abstract public class ResourcedTest {

    public static final String OPAQUEURI = "opaqueURI";
    public static final String MOCK_STORE_READ_ONLY = "mockROnly";
    public static final String MOCK_STORE = "mockStore";
    
    public static String resourceFilePath(String name) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        URL resource = loader.getResource(name);
        if(resource == null) { throw new IllegalStateException("Couldn't find resource " + name); }
        return resource.getFile();
    }

    public static String[] arraySet( String... vals ) {
        return new TreeSet<>(Arrays.asList(vals)).toArray(new String[0]);
    }

    public static ObjectDesc fixture()
    {
        ObjectDesc rec = new ObjectDesc();
        rec.objectName = "newObj";
        rec.storagePlatform = OPAQUEURI;
        rec.directoryPath = "file:///path/to/newObj";
        rec.sizeEstimateBytes = 1234L;
        rec.ownerId = "me";
        rec.readers = arraySet("me","him","her");
        rec.writers = arraySet("me","him","her");
        return rec;
    }
}
