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

import java.net.URL;
import java.util.Arrays;
import java.util.TreeSet;

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;

/**
 * Abstract super-class for tests, that allows easy loading of resources from the classpath.
 */
abstract public class ResourcedTest {

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
        rec.storagePlatform = StoragePlatform.OPAQUEURI.getValue();
        rec.directoryPath = "file:///path/to/newObj";
        rec.sizeEstimateBytes = 1234L;
        rec.ownerId = "me";
        rec.readers = arraySet("me","him","her");
        rec.writers = arraySet("me","him","her");
        return rec;
    }
}
