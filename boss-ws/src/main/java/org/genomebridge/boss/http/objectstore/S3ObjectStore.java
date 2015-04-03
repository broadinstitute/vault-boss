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

        AWSCredentials creds = new BasicAWSCredentials(config.username, config.password);
        client = new AmazonS3Client(creds);
        client.setEndpoint(config.endpoint);
        if ( Boolean.TRUE.equals(config.pathStyleAccess) ) {
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
    public URI generateCopyURI(String bucketAndKey, String locationToCopy, long timeoutInMillis) {
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

    @Override
    public boolean exists(String key) {
        try {
            client.getObjectMetadata(bucket,key);
            return true;
        }
        catch ( Exception e ) {
        }
        return false;
    }

    private AmazonS3 client;
    private String bucket;
}
