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
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.genomebridge.boss.http.resources.StatusResource;

public class BossApplication extends Application<BossConfiguration> {

    public static void main(String[] args) throws Exception {
        new BossApplication().run(args);
    }

    public void run(BossConfiguration config, Environment env) {
        env.jersey().register(new StatusResource());
        env.healthChecks().register("db", new DbHealthCheck());
    }

    public void initialize(Bootstrap<BossConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
    }
}
