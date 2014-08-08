/*
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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ObjectStoreConfiguration {

    private String username, password, endpoint;
    private Boolean pathStyleAccess;
    private String bucket;

    public ObjectStoreConfiguration() {
        File defaultsFile = new File("/etc/s3.conf");
        if(defaultsFile.exists() && defaultsFile.isFile() && defaultsFile.canRead()) {
            try {
                Properties props = new Properties();
                FileInputStream fis = new FileInputStream(defaultsFile);
                props.load(fis);
                fis.close();

                if(props.containsKey("user")) { username = props.getProperty("user"); }
                if(props.containsKey("pass")) { password = props.getProperty("pass"); }

                if((username != null || password != null) &&
                        (username == null || password == null)) {
                    String msg = String.format(
                            "Either username/password must both be set, or neither be set, " +
                            "in the default configuration file %s", defaultsFile.getAbsolutePath());
                    throw new IllegalStateException(msg);
                }

                if(props.containsKey("endpoint")) { endpoint = props.getProperty("endpoint"); }
                if(props.containsKey("pathStyleAccess")) { pathStyleAccess = (boolean)props.get("pathStyleAccess"); }
                if(props.containsKey("bucket")) { bucket = props.getProperty("bucket"); }

            } catch(IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public AmazonS3 createClient() {
        AmazonS3 client = null;

        if(username != null && password != null) {
            AWSCredentials creds = new BasicAWSCredentials(username, password);
            client = new AmazonS3Client(creds);
        } else {
            client = new AmazonS3Client();
        }

        if(endpoint != null) {
            client.setEndpoint(endpoint);
        }

        if(pathStyleAccess != null && pathStyleAccess) {
            client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        }

        return client;
    }

    @JsonProperty
    public Boolean getPathStyleAccess() { return pathStyleAccess; }

    @JsonProperty
    public void setPathStyleAccess(Boolean psa) { this.pathStyleAccess = psa; }

    @JsonProperty
    public String getBucket() { return bucket; }

    @JsonProperty
    public void setBucket(String b) { bucket = b; }

    @JsonProperty
    public String getEndpoint() { return endpoint; }

    @JsonProperty
    public void setEndpoint(String e) { endpoint = e; }

    @JsonProperty
    public String getUsername() { return username; }

    @JsonProperty
    public void setUsername(String u) { username = u; }

    @JsonProperty
    public String getPassword() { return password; }

    @JsonProperty
    public void setPassword(String p) { password = p; }
}
