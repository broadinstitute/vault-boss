package org.genomebridge.boss.http.objectstore;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class FCSObjectStore implements ObjectStore {

    public FCSObjectStore( ObjectStoreConfiguration conf, Client client ) {
        mConf = conf;
        mClient = client;
    }

    @Override
    public URI generateResolveURI(String objKey, String httpMethod, long timeoutInMillis,
                                    String contentType, String contentMD5) {
        try {
            StringBuilder sb = new StringBuilder(mConf.endpoint.length()+mConf.bucket.length()+objKey.length()+2);
            sb.append(mConf.endpoint).append('/').append(mConf.bucket).append('/').append(objKey);
            return new URI(sb.toString());
        }
        catch ( URISyntaxException e ) {
            throw new ObjectStoreException("Bad configuration: "+e.getMessage());
        }
    }

    @Override
    public URI generateCopyURI(String objKey, String locationToCopy, long timeoutInMillis) {
        return generateResolveURI(objKey,HttpMethod.PUT,0L,null,null);
    }

    @Override
    public void deleteObject(String objKey) {
        URI uri = generateResolveURI(objKey,HttpMethod.DELETE,0L,null,null);
        Response response = mClient.target(uri.toString()).request().delete();
        int status = response.getStatus();
        if ( status == Response.Status.OK.getStatusCode() ||
                status == Response.Status.NOT_FOUND.getStatusCode() )
            return;
        throw new ObjectStoreException("Unable to delete object: "+response.readEntity(String.class));
    }

    @Override
    public boolean exists(String objKey) {
        URI uri = generateResolveURI(objKey,HttpMethod.HEAD,0L,null,null);
        Response response = mClient.target(uri.toString()).request().head();
        return response.getStatus() == Response.Status.OK.getStatusCode();
    }

    @Override
    public URI generateResumableUploadURL(String objectName) {
        return generateResolveURI(objectName,HttpMethod.PUT,0L,null,null);
    }

    @Override
    public boolean isReadOnly(){
        return mConf.readOnly;
    }

    private ObjectStoreConfiguration mConf;
    private Client mClient;
}
