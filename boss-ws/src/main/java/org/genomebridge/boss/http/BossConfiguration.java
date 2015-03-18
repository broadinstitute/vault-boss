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

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.genomebridge.boss.http.config.MessageConfiguration;
import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;

import java.util.HashMap;
import java.util.Map;

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

    @Valid
    @JsonProperty
    @NotNull
    private HashMap<String,String> messages;

    private MessageConfiguration messageConfiguration = new MessageConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty
    private ObjectStoreConfiguration localStore = new ObjectStoreConfiguration();

    @JsonProperty
    private ObjectStoreConfiguration cloudStore = new ObjectStoreConfiguration();

    public MessageConfiguration getMessageConfiguration() {
        return messageConfiguration;
    }

    public void setMessages(HashMap<String, String> messages) {
        this.messages = messages;
        messageConfiguration.setMessages(messages);
    }
}
