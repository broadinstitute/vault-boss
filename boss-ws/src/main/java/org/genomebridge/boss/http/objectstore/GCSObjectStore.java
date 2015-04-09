package org.genomebridge.boss.http.objectstore;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.sql.Timestamp;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class GCSObjectStore implements ObjectStore {

    public GCSObjectStore( ObjectStoreConfiguration config ) throws Exception {
        mConfig = config;
    }

    @Override
    public URI generateResolveURI( String objKey, String method, long timeoutInMillis, String contentType, String contentMD5 ) {

        String location = getLocation(objKey);
        return getSignedURI(location,method,timeoutInMillis,contentType,contentMD5,null);
    }

    @Override
    public URI generateCopyURI( String bucketAndKey, String locationToCopy, long timeoutInMillis ) {

        String xHeaders = "x-goog-copy-source:" + locationToCopy + '\n';
        return getSignedURI(bucketAndKey,HttpMethod.PUT,timeoutInMillis,null,null,xHeaders);
    }

    @Override
    public void deleteObject( String objKey ) {
        String location = getLocation(objKey);
        long timeoutInMillis = System.currentTimeMillis() + A_FEW_SECONDS;
        URI uri = getSignedURI(location,HttpMethod.DELETE,timeoutInMillis,null,null,null);
        ClientResponse response = new Client().resource(uri.toString()).delete(ClientResponse.class);
        int status = response.getStatus();
        if ( status == Response.Status.OK.getStatusCode() ||
                status == Response.Status.NO_CONTENT.getStatusCode() ||
                // this is a little iffy, but the client may never have done a PUT
                status == Response.Status.NOT_FOUND.getStatusCode() )
            return;
        throw new ObjectStoreException(response.getEntity(String.class));
    }

    @Override
    public boolean exists( String objKey ) {
        String location = getLocation(objKey);
        long timeoutInMillis = System.currentTimeMillis() + A_FEW_SECONDS;
        URI uri = getSignedURI(location,HttpMethod.HEAD,timeoutInMillis,null,null,null);
        ClientResponse response = new Client().resource(uri.toString()).head();
        return response.getStatus() == Response.Status.OK.getStatusCode();
    }

    @Override
    public URI generateResumableUploadURL(String objectName) {
        URI uri = null;
        try{
            uri = generateResumableURI(objectName);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod(HttpMethod.PUT);
            conn.setRequestProperty("x-goog-resumable", "start");
            conn.getOutputStream().close();
            if (conn.getResponseCode() == Response.Status.CREATED.getStatusCode()) {
                uri = URI.create(conn.getHeaderField("Location"));
            }
        }
        catch (Exception e) {
            throw new ObjectStoreException("Can't get resumable URL. "+e);
        }
        return uri;
    }

    public URI getSignedURI( String location, String method, long timeoutInMillis, String contentType, String contentMD5, String xHeaders ) {

        long timeout = (timeoutInMillis+999L)/1000L;

        StringBuilder sb = new StringBuilder();
        sb.append(method).append('\n');
        if ( contentMD5 != null ) sb.append(contentMD5);
        sb.append('\n');
        if ( contentType != null ) sb.append(contentType);
        sb.append('\n');
        sb.append(timeout).append('\n');
        if ( xHeaders != null ) sb.append(xHeaders);
        sb.append(location);

        String sig;
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(getKey());
            signer.update(sb.toString().getBytes(StandardCharsets.UTF_8));
            sig = URLEncoder.encode(DatatypeConverter.printBase64Binary(signer.sign()),StandardCharsets.UTF_8.name());
        }
        catch ( Exception e ) {
            throw new ObjectStoreException("Can't encrypt signature.",e);
        }

        sb.setLength(0);
        sb.append(mConfig.endpoint).append(location)
            .append("?GoogleAccessId=").append(mConfig.username)
            .append("&Expires=").append(timeout)
            .append("&Signature=").append(sig);
        return URI.create(sb.toString());
    }

    private String getLocation( String objKey ) {

        StringBuilder sb = new StringBuilder(mConfig.bucket.length()+objKey.length()+2);
        sb.append('/').append(mConfig.bucket).append('/').append(objKey);
        return sb.toString();
    }

    private PrivateKey getKey() throws Exception {
        if ( mKey == null ) {
            final char[] password = "notasecret".toCharArray();
            FileInputStream fis = new FileInputStream(mConfig.password);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, password);
            fis.close();
            mKey = (PrivateKey)ks.getKey("privatekey", password);
        }
        return mKey;
    }



    private URI generateResumableURI(String name) throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long timeout = now.getTime() + 1000L * 1000;
        String location = getLocation(name);
        String xHeaders = "x-goog-resumable:start" + '\n';
        return getSignedURI(location, "PUT", timeout, null, null, xHeaders);
    }

    private ObjectStoreConfiguration mConfig;
    private PrivateKey mKey;
    private static final long A_FEW_SECONDS = 5000L;
}
