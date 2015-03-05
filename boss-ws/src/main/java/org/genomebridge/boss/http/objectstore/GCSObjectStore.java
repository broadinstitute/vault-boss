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
    public URI generatePresignedURL( String objectId, String method, long timeoutInMillis, String contentType, String contentMD5 ) {

        long timeout = (timeoutInMillis+999L)/1000L;

        StringBuilder sb = new StringBuilder();
        sb.append('/').append(mConfig.bucket).append('/').append(objectId);
        String location = sb.toString();

        sb.setLength(0);
        sb.append(method).append('\n');
        if ( contentMD5 != null ) sb.append(contentMD5);
        sb.append('\n');
        if ( contentType != null ) sb.append(contentType);
        sb.append('\n');
        sb.append(timeout).append('\n');
        sb.append(location);

        String sig;
        try {
            if ( mKey == null ) {
                final char[] password = "notasecret".toCharArray();
                FileInputStream fis = new FileInputStream(mConfig.password);
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(fis, password);
                fis.close();
                mKey = (PrivateKey)ks.getKey("privatekey", password);
            }
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(mKey);
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

    @Override
    public void deleteObject( String objectId ) {
        URI uri = generatePresignedURL(objectId,HttpMethod.DELETE,System.currentTimeMillis()+1000L,null,null);
        ClientResponse response = new Client().resource(uri.toString()).delete(ClientResponse.class);
        int status = response.getStatus();
        if ( status == Response.Status.OK.getStatusCode() ||
                status == Response.Status.NO_CONTENT.getStatusCode() ||
                // this is a little iffy, but the client may never have done a PUT
                status == Response.Status.NOT_FOUND.getStatusCode() )
            return;
        throw new ObjectStoreException(response.getEntity(String.class));
    }

    private ObjectStoreConfiguration mConfig;
    private PrivateKey mKey;
}
