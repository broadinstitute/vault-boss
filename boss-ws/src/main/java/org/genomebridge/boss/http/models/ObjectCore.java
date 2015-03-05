/*
 * Copyright 2015 Broad Institute
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
package org.genomebridge.boss.http.models;

public class ObjectCore {

    public void copy( ObjectCore that ) {
        this.objectId = that.objectId;
        this.objectName = that.objectName;
        this.storagePlatform = that.storagePlatform;
        this.directoryPath = that.directoryPath;
        this.sizeEstimateBytes = that.sizeEstimateBytes;
        this.ownerId = that.ownerId;
    }

    public String objectId;
    public String objectName;
    public String storagePlatform;
    public String directoryPath;
    public Long sizeEstimateBytes;
    public String ownerId;
}