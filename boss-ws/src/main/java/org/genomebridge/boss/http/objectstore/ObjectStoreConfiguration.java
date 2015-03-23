package org.genomebridge.boss.http.objectstore;

/**
 * Configuration for the object store behind the BOSS API.  This is configured
 * using an objectStore clause in the YAML configuration file.
 */
public class ObjectStoreConfiguration {

    public String username;
    public String password;
    public String endpoint;
    public Boolean pathStyleAccess;
    public String bucket;
    public String type; // currently either S3 or GCS
}
