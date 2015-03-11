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
