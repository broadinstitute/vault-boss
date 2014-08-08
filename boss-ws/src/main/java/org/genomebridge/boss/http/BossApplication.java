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

import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.resources.AllGroupsResource;
import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.StatusResource;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.skife.jdbi.v2.DBI;

public class BossApplication extends Application<BossConfiguration> {

    public static void main(String[] args) throws Exception {
        new BossApplication().run(args);
    }

    public void run(BossConfiguration config, Environment env) {
        env.jersey().register(StatusResource.class);

        final DBIFactory factory = new DBIFactory();

        try {
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            final BossDAO dao = jdbi.onDemand(BossDAO.class);

            env.jersey().register(GroupResource.class);
            env.jersey().register(FsGroupResource.class);
            env.jersey().register(AllGroupsResource.class);

        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);

            throw new IllegalStateException("Couldn't start up the application", e);
        }

        env.healthChecks().register("db", new DbHealthCheck());
    }

    public void initialize(Bootstrap<BossConfiguration> bootstrap) {

        GuiceBundle<BossConfiguration> guiceBundle = GuiceBundle.<BossConfiguration>newBuilder()
                .addModule(new BossModule())
                .setConfigClass(BossConfiguration.class)
                .build();

        bootstrap.addBundle(guiceBundle);

        bootstrap.addBundle(new MigrationsBundle<BossConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(BossConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
    }
}
