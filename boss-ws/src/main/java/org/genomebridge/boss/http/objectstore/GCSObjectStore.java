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
package org.genomebridge.boss.http.objectstore;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;

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
    public URI generateCopyURI( String objKey, String locationToCopy ) {

        String location = getLocation(objKey);
        long timeoutInMillis = System.currentTimeMillis() + A_FEW_SECONDS;
        String xHeaders = "x-goog-copy-source:" + locationToCopy + '\n';
        return getSignedURI(location,HttpMethod.PUT,timeoutInMillis,null,null,xHeaders);
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

    private ObjectStoreConfiguration mConfig;
    private PrivateKey mKey;
    private static final long A_FEW_SECONDS = 5000L;
}
