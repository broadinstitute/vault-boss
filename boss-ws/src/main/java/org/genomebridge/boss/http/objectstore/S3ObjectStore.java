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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.util.Base64;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
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
    public URI generatePresignedURL(String key, HttpMethod method, long timeoutInMillis,
                                    String contentType, byte[] contentMD5) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, awsMethod(method));
        request.setExpiration(new Date(System.currentTimeMillis() + timeoutInMillis));
        if (contentType != null) {
            request.setContentType(contentType);
        }
        if (contentMD5 != null) {
            request.setContentMd5(Base64.encodeAsString(contentMD5));
        }
        URL url = client.generatePresignedUrl(request);
        return URI.create(url.toString());
    }

    @Override
    public void deleteObject(String key) throws ObjectStoreException {
        try {
            client.deleteObject(bucket, key);
        } catch (AmazonClientException ace) {
            throw new ObjectStoreException(ace);
        }
    }

    @Override
    public String initiateMultipartUpload(String key) {
        return client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket,key)).getUploadId();
    }

    @Override
    public URI getMultipartUploadURL(String key, String uploadId, int partNumber, long timeoutInMillis,
                                        String contentType, String contentMD5) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, com.amazonaws.HttpMethod.PUT);
        request.addRequestParameter("PartNumber", Integer.toString(partNumber));
        request.addRequestParameter("UploadId", uploadId);
        request.setExpiration(new Date(System.currentTimeMillis() + timeoutInMillis));
        if (contentType != null) {
            request.setContentType(contentType);
        }
        if (contentMD5 != null) {
            request.setContentMd5(contentMD5);
        }
        URL url = client.generatePresignedUrl(request);
        return URI.create(url.toString());
    }

    @Override
    public String commitMultipartUpload(String key, String uploadId, String[] eTags) {
        ArrayList<PartETag> partETags = new ArrayList<>(eTags.length);
        for ( String eTag : eTags )
            partETags.add(new PartETag(partETags.size(),eTag));
        CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(bucket,key,uploadId,partETags);
        return client.completeMultipartUpload(req).getETag();
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        AbortMultipartUploadRequest req = new AbortMultipartUploadRequest(bucket,key,uploadId);
        client.abortMultipartUpload(req);
    }
}
