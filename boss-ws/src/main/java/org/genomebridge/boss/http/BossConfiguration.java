package org.genomebridge.boss.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;

import java.util.HashMap;

public class BossConfiguration extends Configuration {

    public BossConfiguration() {}

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public ObjectStoreConfiguration getLocalStoreConfiguration() {
        return localStore;
    }

    public ObjectStoreConfiguration getCloudStoreConfiguration() {
        return cloudStore;
    }

    public HashMap<String,String> getMessages() {
        return messages;
    }

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty
    private ObjectStoreConfiguration localStore = new ObjectStoreConfiguration();

    @JsonProperty
    private ObjectStoreConfiguration cloudStore = new ObjectStoreConfiguration();

    @Valid
    @JsonProperty
    @NotNull
    private HashMap<String,String> messages;
}
