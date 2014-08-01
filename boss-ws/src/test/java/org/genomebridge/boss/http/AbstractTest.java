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
package org.genomebridge.boss.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.boss.http.resources.FsGroupResource;
import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

abstract public class AbstractTest {

    abstract public DropwizardAppRule<BossConfiguration> rule();

    public static String resourceFilePath(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resource = loader.getResource(name);
        return resource != null ? resource.getFile() : null;
    }

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> ClientResponse post(Client client, String url, T value) { return post(client, url, "testuser", value); }

    public <T> ClientResponse post(Client client, String url, String user, T value) {
        return client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                .post(ClientResponse.class, value);
    }

    public ClientResponse delete(Client client, String url) { return delete(client, url, "testuser"); }

    public ClientResponse delete(Client client, String url, String user) {
        return client.resource(url)
                .header("REMOTE_USER", user)
                .delete(ClientResponse.class);
    }

    public ClientResponse get(Client client, String url) { return get(client, url, "testuser"); }

    public ClientResponse get(Client client, String url, String user) {
        return client.resource(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                .get(ClientResponse.class);
    }


    public ClientResponse check200( ClientResponse response ) {
        return checkStatus(200, response);
    }

    public ClientResponse checkStatus( int status, ClientResponse response ) {
        assertThat(response.getStatus()).isEqualTo(status);
        return response;
    }

    public String randomID() {
        return UUID.randomUUID().toString();
    }

    /*
     * Utility methods for interacting with the BOSS API
     */

    public String fsGroupPath(String groupId) {
        return String.format("http://localhost:%d/group/fs/%s", rule().getLocalPort(), groupId);
    }
    public String groupPath(String groupId) {
        return String.format("http://localhost:%d/group/store/%s", rule().getLocalPort(), groupId);
    }

    public String fsObjectPath(String objectId, String groupId) {
        return String.format("%s/%s", fsGroupPath(groupId), objectId);
    }
    public String objectPath(String objectId, String groupId) {
        return String.format("%s/%s", groupPath(groupId), objectId);
    }

    public static String[] arraySet( String... vals ) {
        return new TreeSet<>(Arrays.asList(vals)).toArray(new String[0]);
    }

    public static String[] arrayAppend( String[] array, String newValue ) {
        TreeSet<String> set = new TreeSet<>(Arrays.asList(array));
        set.add(newValue);
        return set.toArray(new String[0]);
    }

    public ClientResponse createFsGroup(String groupId, String owner, String directory) {
        Client client = new Client();
        FsGroupResource grp = new FsGroupResource();
        grp.ownerId = owner;
        grp.readers = arraySet( "testuser", owner );
        grp.writers = arraySet( "testuser", owner );
        grp.directory = directory;

        String groupPath = fsGroupPath(groupId);

        return post(client, groupPath, grp);
    }

    public ClientResponse createFsObject(String objectId, String groupId,
                                         String name, String owner, long sizeEstimateBytes) {
        Client client = new Client();

        FsObjectResource obj = new FsObjectResource();
        obj.name = name;
        obj.ownerId = owner;
        obj.readers = arraySet( owner, "testuser" );
        obj.writers = arraySet( owner, "testuser" );
        obj.sizeEstimateBytes = sizeEstimateBytes;

        String objectPath = fsObjectPath(objectId, groupId);

        return post(client, objectPath, obj);
    }

    public ClientResponse createGroup(String groupId, String owner, String typeHint, Long sizeEstimate) {
        Client client = new Client();
        GroupResource grp = new GroupResource();
        grp.ownerId = owner;
        grp.readers = arraySet( "testuser", owner );
        grp.writers = arraySet( "testuser", owner );
        grp.typeHint = typeHint;
        grp.sizeEstimateBytes = sizeEstimate;

        String groupPath = groupPath(groupId);

        return post(client, groupPath, grp);
    }

    public ClientResponse createObject(String objectId, String groupId,
                                         String name, String owner, long sizeEstimateBytes) {
        Client client = new Client();

        ObjectResource obj = new ObjectResource();
        obj.name = name;
        obj.ownerId = owner;
        obj.readers = arraySet( owner, "testuser" );
        obj.writers = arraySet( owner, "testuser" );
        obj.sizeEstimateBytes = sizeEstimateBytes;

        String objectPath = objectPath(objectId, groupId);

        return post(client, objectPath, obj);
    }

}
