package org.genomebridge.boss.http.objectstore;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.net.URI;
import java.net.URL;
import java.util.Date;

public class S3ObjectStore implements ObjectStore {

    public S3ObjectStore( ObjectStoreConfiguration config ) {
        bucket = config.bucket;

        if ( config.username != null && config.password != null ) {
            AWSCredentials creds = new BasicAWSCredentials(config.username, config.password);
            client = new AmazonS3Client(creds);
        } else {
            client = new AmazonS3Client();
        }
        if ( config.endpoint != null ) {
            client.setEndpoint(config.endpoint);
        }
        if ( config.pathStyleAccess != null && config.pathStyleAccess ) {
            client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        }
    }

    @Override
    public URI generateResolveURI(String key, String httpMethod, long timeoutInMillis,
                                    String contentType, String contentMD5) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.valueOf(httpMethod));
        request.setExpiration(new Date(timeoutInMillis));
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
    public URI generateCopyURI(String key, String locationToCopy, long timeoutInMillis) {
        throw new ObjectStoreException("Copying objects is not currently supported on S3 storage.");
    }

    @Override
    public void deleteObject(String key) throws ObjectStoreException {
        try {
            client.deleteObject(bucket, key);
        } catch (AmazonClientException ace) {
            throw new ObjectStoreException(ace);
        }
    }

    private AmazonS3 client;
    private String bucket;
}
