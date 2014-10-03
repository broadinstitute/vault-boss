/**
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

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.genomebridge.boss.http.objectstore.S3ObjectStore;
import org.genomebridge.boss.http.resources.AllObjectsResource;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.BossAPIProvider;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.skife.jdbi.v2.DBI;

/**
 * Top-level entry point to the entire application.
 *
 * See the Dropwizard docs here:
 *   https://dropwizard.github.io/dropwizard/manual/core.html
 *
 */
public class BossApplication extends Application<BossConfiguration> {

    public static void main(String[] args) throws Exception {
        new BossApplication().run(args);
    }

    public void run(BossConfiguration config, Environment env) {

        /*
        Set up the Boss API, which includes the object store, here. Then stash that API into the BossAPIProvider
        singleton. We manage this singleton ourselves, instead of relying on dependency injection, due to problems
        with Dropwizard + Guice lifecycle. See https://github.com/HubSpot/dropwizard-guice/issues/19 for discussion
        of those lifecycle problems.

        Furthermore, creating the JDBI objects in the run() method properly registers health checks and metrics
        for the JDBI connection pool. When we tried creating the JDBI objects elsewhere, the db pool metrics
        did not work correctly; we did not investigate workarounds for this.
         */
        try {
            // JDBI
            final DBIFactory factory = new DBIFactory();
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            final BossDAO dao = jdbi.onDemand(BossDAO.class);

            // Object store
            ObjectStoreConfiguration osConfig = config.getObjectStoreConfiguration();
            ObjectStore store = new S3ObjectStore(osConfig.createClient(), osConfig.getBucket());

            // BOSS API
            BossAPI api = new DatabaseBossAPI(dao, store);

            // stash in singleton
            BossAPIProvider.getInstance().setApi(api);

        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
        }

        /*
        Set up the resources themselves.
         */
        env.jersey().register(ObjectResource.class);
        env.jersey().register(AllObjectsResource.class);
    }

    public void initialize(Bootstrap<BossConfiguration> bootstrap) {

        bootstrap.addBundle(new MigrationsBundle<BossConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(BossConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
    }
}
