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
import org.genomebridge.boss.http.resources.GroupResource;
import org.genomebridge.boss.http.resources.ObjectResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    abstract public DropwizardAppRule<BossConfiguration> rule();

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

    public String checkHeader( ClientResponse response, String header ) {
        MultivaluedMap<String,String> map = response.getHeaders();
        assertThat(map).describedAs(String.format("header \"%s\"", header)).containsKey(header);
        return map.getFirst(header);
    }

    public String randomID() {
        return UUID.randomUUID().toString();
    }

    /*
     * Utility methods for interacting with the BOSS API
     */

    public String groupPath(String groupId) {
        return String.format("http://localhost:%d/group/%s", rule().getLocalPort(), groupId);
    }

    public String groupsPath() {
        return String.format("http://localhost:%d/groups", rule().getLocalPort());
    }

    public String objectPath(String objectId, String groupId) {
        return String.format("%s/object/%s", groupPath(groupId), objectId);
    }
    public String resolveObjectPath(String objectId, String groupId) {
        return String.format("%s/resolve", objectPath(objectId, groupId));
    }

    public String objectsPath(String groupId) {
        return String.format("%s/objects", groupPath(groupId));
    }

    public static String[] arraySet( String... vals ) {
        return new TreeSet<>(Arrays.asList(vals)).toArray(new String[0]);
    }

    public static String[] arrayAppend( String[] array, String newValue ) {
        TreeSet<String> set = new TreeSet<>(Arrays.asList(array));
        set.add(newValue);
        return set.toArray(new String[0]);
    }

    public ClientResponse createGroup(String groupId, String owner, String typeHint, Long sizeEstimate) {
        Client client = new Client();
        GroupResource grp = new GroupResource();
        grp.ownerId = owner;
        grp.readers = arraySet( "testuser", owner );
        grp.writers = arraySet( "testuser", owner );
        grp.typeHint = typeHint;
        grp.sizeEstimateBytes = sizeEstimate;
        grp.storagePlatform = "objectstore";

        String groupPath = groupPath(groupId);

        return post(client, groupPath, grp);
    }

    public ClientResponse createAnonymousGroup(String owner, String typeHint, Long sizeEstimate) {
        Client client = new Client();
        GroupResource grp = new GroupResource();
        grp.ownerId = owner;
        grp.readers = arraySet( "testuser", owner );
        grp.writers = arraySet( "testuser", owner );
        grp.typeHint = typeHint;
        grp.sizeEstimateBytes = sizeEstimate;
        grp.storagePlatform = "objectstore";

        String groupPath = groupsPath();

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
        obj.storagePlatform = "objectstore";

        String objectPath = objectPath(objectId, groupId);

        return post(client, objectPath, obj);
    }

    public ClientResponse createAnonymousObject(String groupId, String name, String owner, long sizeEstimateBytes) {
        Client client = new Client();

        ObjectResource obj = new ObjectResource();
        obj.name = name;
        obj.ownerId = owner;
        obj.readers = arraySet( owner, "testuser" );
        obj.writers = arraySet( owner, "testuser" );
        obj.sizeEstimateBytes = sizeEstimateBytes;
        obj.storagePlatform = "objectstore";

        String objectPath = objectsPath(groupId);

        return post(client, objectPath, obj);
    }

}
