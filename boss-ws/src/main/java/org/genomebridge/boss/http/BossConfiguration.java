package org.genomebridge.boss.http;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BossConfiguration extends Configuration {

    public BossConfiguration() {}

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();
    
    @Valid
    @NotNull
    @JsonProperty
    private Map<String,ObjectStoreConfiguration> objectStores = new  HashMap<String,ObjectStoreConfiguration>();

	public Map<String, ObjectStoreConfiguration> getObjectStores() {
		return objectStores;
	}

	public void setObjectStores(Map<String, ObjectStoreConfiguration> objectStores) {
		this.objectStores = objectStores;
	}

	
}
