package org.genomebridge.boss.http.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.genomebridge.boss.http.BossConfiguration;
import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.skife.jdbi.v2.DBI;

/**
 * Created by davidan on 9/16/14.
 *
 * A singleton to create and manage the BossAPI instance to be used by the application.
 *
 * We want the BossAPI instance to be a singleton, because it contains the BossDAO and ObjectStore,
 * which should in turn be singletons. For instance, the BossDAO manages the db connection pool - if
 * it wasn't a singleton, we'd spin up a new DB connection for every request.
 *
 * This singleton is managed by Guice.
 *
 */
@Singleton
public class BossAPIProvider {

    private final BossAPI api;

    @Inject
    public BossAPIProvider(Environment env, BossConfiguration config, ObjectStore store) {
        final DBIFactory factory = new DBIFactory();
        try {
            final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
            final BossDAO dao = jdbi.onDemand(BossDAO.class);

            this.api = new DatabaseBossAPI(dao, store);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public BossAPI getApi() {
        return api;
    }

}
