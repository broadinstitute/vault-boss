package org.genomebridge.boss.http;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.db.BossDAO;
import org.genomebridge.boss.http.objectstore.GCSObjectStore;
import org.genomebridge.boss.http.objectstore.ObjectStore;
import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;
import org.genomebridge.boss.http.objectstore.S3ObjectStore;
import org.genomebridge.boss.http.resources.AllObjectsResource;
import org.genomebridge.boss.http.resources.ObjectResource;
import org.genomebridge.boss.http.service.BossAPI;
import org.genomebridge.boss.http.service.DatabaseBossAPI;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level entry point to the entire application.
 *
 * See the Dropwizard docs here:
 *   https://dropwizard.github.io/dropwizard/manual/core.html
 *
 */
public class BossApplication extends Application<BossConfiguration> {

    public static void main(String[] args) throws Exception {
        try {
            new BossApplication().run(args);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void run(BossConfiguration config, Environment env) throws Exception {

        // Create an API object that the resources can use.
        gDBI = new DBIFactory().build(env, config.getDataSourceFactory(), "db");
        gDBI.registerArgumentFactory(new NullArgumentFactory());
        Map<String,ObjectStore> objectStores = getObjectStoresMap(config);
        gBossAPI = new DatabaseBossAPI(gDBI,objectStores,getMessages());
       // Set up the resources themselves.
        env.jersey().register(new ObjectResource(gBossAPI));
        env.jersey().register(new AllObjectsResource(gBossAPI));


    }

    private Map<String, ObjectStore> getObjectStoresMap(BossConfiguration config) throws Exception {
    	
    	Map<String,ObjectStoreConfiguration> objectStoreConfigurationMap = config.getObjectStores();
    	Map<String,ObjectStore> objectStoreMap = new HashMap<String,ObjectStore>();
    	
    	if(objectStoreConfigurationMap != null){
    		for (Map.Entry<String, ObjectStoreConfiguration> entry : objectStoreConfigurationMap.entrySet()) {
    			objectStoreMap.put(entry.getKey(), getObjectStore(entry.getValue()));
    		}
    	}
    	
		return objectStoreMap;
	}

	// For invoking some liquibase magic when the args to the server invocation so specify.
    public void initialize(Bootstrap<BossConfiguration> bootstrap) {

        bootstrap.addBundle(new MigrationsBundle<BossConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(BossConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
    }

    // These next two little methods break encapsulation, and are just for unit testing.
    public static BossDAO getDAO() {
        return gDBI.onDemand(BossDAO.class);
    }
    public static BossAPI getAPI() {
        return gBossAPI;
    }

    private static ObjectStore getObjectStore( ObjectStoreConfiguration config ) throws Exception {
        if ( "S3".equals(config.type) )
            return new S3ObjectStore(config);
        if ( "GCS".equals(config.type) )
            return new GCSObjectStore(config);
        throw new IllegalStateException("ObjectStore configuration has unrecognized type: "+config.type);
    }

    private static class BossMessages {
        @Valid
        @JsonProperty
        @NotNull
        public HashMap<String,String> messages;
    }

    public static synchronized Map<String,String> getMessages() {
        if ( gMessages == null ) {
            try (InputStream messageInput = ClassLoader.getSystemResourceAsStream(MESSAGES_FILE)) {
                gMessages = Collections.unmodifiableMap(new Yaml().loadAs(messageInput,BossMessages.class).messages);
            }
            catch ( IOException e ) {
                throw new IllegalStateException("Couldn't load resource " + MESSAGES_FILE,e);
            }
        }
        return gMessages;
    }

    // Workaround for some null argument funkiness in JDBI that breaks on Oracle.
    private static class NullArgumentFactory implements ArgumentFactory<Object> {

        @Override
        public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx) {
            return value == null;
        }

        @Override
        public Argument build(Class<?> expectedType, Object value, StatementContext ctx) {
            return new NullArgument();
        }

        private static class NullArgument implements Argument {
            @Override
            public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
                statement.setNull(position, Types.NULL);
            }
        }
    }

    private static DBI gDBI;
    private static BossAPI gBossAPI;
    private static Map<String,String> gMessages;
    private static final String MESSAGES_FILE = "messages.yml";
}
