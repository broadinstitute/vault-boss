package org.genomebridge.boss.http;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;
import org.genomebridge.boss.http.swagger.SwaggerConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BossConfiguration extends Configuration {

    public BossConfiguration() {}

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }



    public SwaggerConfiguration getSwaggerConfiguration() {return swagger;}

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();
    
    @Valid
    @NotNull
    @JsonProperty
    private Map<String,ObjectStoreConfiguration> objectStores = new  HashMap<String,ObjectStoreConfiguration>();

    @Valid
    @NotNull
    @JsonProperty
    private ObjectStoreConfiguration cloudStore = new ObjectStoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private SwaggerConfiguration swagger = new SwaggerConfiguration();

	public Map<String, ObjectStoreConfiguration> getObjectStores() {
		return objectStores;
	}

	public void setObjectStores(Map<String, ObjectStoreConfiguration> objectStores) {
		this.objectStores = objectStores;
	}

	
}
