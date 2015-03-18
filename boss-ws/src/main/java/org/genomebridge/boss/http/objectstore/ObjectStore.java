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

import java.net.URI;

/**
 * A wrapper around different object store interfaces (the two of which we deal with, now, are S3-compliant,
 * but we might need to deal with other object stores in the future) so that we can generate pre-signed URLs.
 */
public interface ObjectStore {

    public URI generateResolveURI(String objKey, String httpMethod, long timeoutInMillis,
                                    String contentType, String contentMD5);

    public URI generateCopyURI(String objKey, String locationToCopy);

    public void deleteObject(String objKey);
}
