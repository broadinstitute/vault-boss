package org.genomebridge.boss.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;
import org.genomebridge.boss.http.swagger.SwaggerConfiguration;

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

    public SwaggerConfiguration getSwaggerConfiguration() {return swagger;}

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private ObjectStoreConfiguration localStore = new ObjectStoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private ObjectStoreConfiguration cloudStore = new ObjectStoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private SwaggerConfiguration swagger = new SwaggerConfiguration();
}
