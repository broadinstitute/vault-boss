package org.genomebridge.boss.http.objectstore;

import javax.validation.constraints.NotNull;

/**
 * Configuration for the object store behind the BOSS API.  This is configured
 * using an objectStore clause in the YAML configuration file.
 */

public class ObjectStoreConfiguration {

    @NotNull
    public String username;

    @NotNull
    public String password;

    @NotNull
    public String endpoint;

    public Boolean pathStyleAccess;

    @NotNull
    public String bucket;

    public enum ObjectStoreType {
        S3,  // Amazon S3 or ECS
        GCS, // Google Cloud Storage
        FCS  // Faux Cloud Storage
    }

    @NotNull
    public ObjectStoreType type;

    public boolean readOnly;

}
