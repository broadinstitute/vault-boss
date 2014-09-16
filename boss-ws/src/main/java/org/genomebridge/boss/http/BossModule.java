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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.genomebridge.boss.http.objectstore.S3ObjectStore;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.skife.jdbi.v2.DBI;

public class BossModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    public ObjectStore providesObjectStore(Environment env, BossConfiguration config) {
        ObjectStoreConfiguration osConfig = config.getObjectStoreConfiguration();
        return new S3ObjectStore(osConfig.createClient(), osConfig.getBucket());
    }

}
