package org.genomebridge.boss.http.objectstore;

import javax.validation.constraints.NotNull;

/**
 * Configuration for the object store behind the BOSS API.  This is configured
 * using an objectStore clause in the YAML configuration file.
 */

public class ObjectStoreConfiguration {

    public String username;

    public String password;

    @NotNull
    public String endpoint;

    public Boolean pathStyleAccess;

    @NotNull
    public String bucket;

    @NotNull
    public String type; // currently either S3 or GCS

    public Boolean readOnly;
}
