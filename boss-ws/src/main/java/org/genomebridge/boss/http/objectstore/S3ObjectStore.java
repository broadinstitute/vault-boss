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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.net.URI;
import java.net.URL;
import java.util.Date;

public class S3ObjectStore implements ObjectStore {

    private AmazonS3 client;
    private String bucket;

    public S3ObjectStore(AmazonS3 client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    private com.amazonaws.HttpMethod awsMethod( HttpMethod method ) {
        switch(method) {
            case GET: return com.amazonaws.HttpMethod.GET;
            case HEAD: return com.amazonaws.HttpMethod.HEAD;
            case PUT: return com.amazonaws.HttpMethod.PUT;
            case DELETE: return com.amazonaws.HttpMethod.DELETE;
            default: throw new IllegalArgumentException(
                    String.format("No AWS Method equivalent for method %s", method.toString()) );
        }
    }

    @Override
    public URI generatePresignedURL(String key, HttpMethod method, long timeoutInMillis) {
        URL url = client.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket, key, awsMethod(method))
                .withExpiration(new Date(System.currentTimeMillis() + timeoutInMillis)));
        return URI.create(url.toString());
    }
}
