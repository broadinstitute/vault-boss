package org.genomebridge.boss.http.service;

/**
 * Created by davidan on 9/16/14.
 *
 * A singleton to contain the BossAPI instance to be used by the application.
 *
 * We want the BossAPI instance to be a singleton, because it contains the BossDAO and ObjectStore,
 * which should in turn be singletons. For instance, the BossDAO manages the db connection pool - if
 * it wasn't a singleton, we'd spin up a new DB connection for every request.
 *
 *
 */
public class BossAPIProvider {

    // singleton management code
    private BossAPIProvider() {}

    private static class BossAPIProviderHolder {
        public static BossAPIProvider INSTANCE = new BossAPIProvider();
    }

    public static BossAPIProvider getInstance() {
        return BossAPIProviderHolder.INSTANCE;
    }

    // api
    private BossAPI api;

    public void setApi(BossAPI api) {
        this.api = api;
    }

    public BossAPI getApi() {
        return this.api;
    }

}
