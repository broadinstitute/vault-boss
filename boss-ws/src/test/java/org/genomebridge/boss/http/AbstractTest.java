package org.genomebridge.boss.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import io.dropwizard.testing.junit.DropwizardAppRule;

import org.genomebridge.boss.http.models.StoragePlatform;
import org.genomebridge.boss.http.resources.AbstractResource;
import org.genomebridge.boss.http.service.BossAPI.ObjectDesc;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    abstract public DropwizardAppRule<BossConfiguration> rule();
    public static final String REMOTE_USER_HEADER = AbstractResource.REMOTE_USER_HEADER;

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> ClientResponse post(Client client, String url, T value) { return post(client, url, "testuser", value); }

    public <T> ClientResponse post(Client client, String url, String user, T value) {
        return client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(REMOTE_USER_HEADER, user)
                .post(ClientResponse.class, value);
    }

    public ClientResponse delete(Client client, String url) { return delete(client, url, "testuser"); }

    public ClientResponse delete(Client client, String url, String user) {
        return client.resource(url)
                .header(REMOTE_USER_HEADER, user)
                .delete(ClientResponse.class);
    }

    public ClientResponse get(Client client, String url) { return get(client, url, "testuser"); }

    public ClientResponse get(Client client, String url, String user) {
        return client.resource(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(REMOTE_USER_HEADER, user)
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

    public String basePath() {
        return String.format("http://localhost:%d", rule().getLocalPort());
    }

    public String objectsPath() {
        return String.format("%s/objects", basePath());
    }

    public String objectPath(String objectId) {
        return String.format("%s/objects/%s", basePath(), objectId);
    }
    public String resolveObjectPath(String objectId) {
        return String.format("%s/resolve", objectPath(objectId));
    }

    public static String[] arrayAppend( String[] array, String newValue ) {
        TreeSet<String> set = new TreeSet<>(Arrays.asList(array));
        set.add(newValue);
        return set.toArray(new String[0]);
    }

    public ClientResponse createObject(String objectName, String owner, long size) {
        return createObject(objectName, owner, StoragePlatform.LOCALSTORE.getValue(), null, size);
    }

    public ClientResponse createObject(String objectName, String owner, String platform, String path, long size) {
        Client client = new Client();

        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = owner;
        obj.objectName = objectName;
        obj.readers = arraySet( owner, "testuser" );
        obj.writers = arraySet( owner, "testuser" );
        obj.sizeEstimateBytes = size;
        obj.storagePlatform = platform;
        obj.directoryPath = path;

        String objectsPath = objectsPath();

        return post(client, objectsPath, obj);
    }

}
