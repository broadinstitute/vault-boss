package org.genomebridge.boss.http.objectstore;

import java.net.URI;

/**
 * A wrapper around different object store interfaces (the two of which we deal with, now, are S3-compliant,
 * but we might need to deal with other object stores in the future) so that we can generate pre-signed URLs.
 */
public interface ObjectStore {

    public URI generateResolveURI(String objKey, String httpMethod, long timeoutInMillis,
                                    String contentType, String contentMD5);

    public URI generateCopyURI(String bucketAndKey, String locationToCopy, long timeoutInMillis);

    public void deleteObject(String objKey);

    public boolean exists(String objKey);

    public URI generateResumableUploadURL(String objectName);
}
