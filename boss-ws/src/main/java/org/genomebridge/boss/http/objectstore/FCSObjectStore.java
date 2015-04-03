package org.genomebridge.boss.http.objectstore;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class FCSObjectStore implements ObjectStore {

    public FCSObjectStore( ObjectStoreConfiguration conf ) {
        mConf = conf;
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
        ClientResponse response = new Client().resource(uri.toString()).delete(ClientResponse.class);
        int status = response.getStatus();
        if ( status == Response.Status.OK.getStatusCode() ||
                status == Response.Status.NOT_FOUND.getStatusCode() )
            return;
        throw new ObjectStoreException("Unable to delete object: "+response.getEntity(String.class));
    }

    @Override
    public boolean exists(String objKey) {
        URI uri = generateResolveURI(objKey,HttpMethod.HEAD,0L,null,null);
        ClientResponse response = new Client().resource(uri.toString()).head();
        return response.getStatus() == Response.Status.OK.getStatusCode();
    }

    private ObjectStoreConfiguration mConf;
}
