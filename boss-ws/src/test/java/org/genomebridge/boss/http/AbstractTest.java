package org.genomebridge.boss.http;

import static org.fest.assertions.api.Assertions.assertThat;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.resources.AbstractResource;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    abstract public DropwizardAppRule<BossConfiguration> rule();
    public static final String REMOTE_USER_HEADER = AbstractResource.REMOTE_USER_HEADER;
    public static final String OPAQUEURI = "opaqueURI";
    public static final String MOCK_STORE_READ_ONLY = "mockROnly";


    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> Response post(Client client, String url, T value) { return post(client, url, "testuser", value); }

    public <T> Response post(Client client, String url, String user, T value) {
        return client.target(url).request(MediaType.APPLICATION_JSON_TYPE)
                .header(REMOTE_USER_HEADER, user)
                .post(Entity.entity(value,MediaType.APPLICATION_JSON_TYPE));
    }

    public Response delete(Client client, String url) { return delete(client, url, "testuser"); }

    public Response delete(Client client, String url, String user) {
        return client.target(url).request()
                .header(REMOTE_USER_HEADER, user)
                .delete();
    }

    public Response get(Client client, String url) { return get(client, url, "testuser"); }

    public Response get(Client client, String url, String user) {
        return client.target(url)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(REMOTE_USER_HEADER, user)
                .get();
    }


    public Response check200( Response response ) {
        return checkStatus(200, response);
    }

    public Response checkStatus( int status, Response response ) {
        assertThat(response.getStatus()).isEqualTo(status);
        return response;
    }

    public String checkHeader( Response response, String header ) {
        MultivaluedMap<String,Object> map = response.getHeaders();
        assertThat(map).describedAs(String.format("header \"%s\"", header)).containsKey(header);
        return map.getFirst(header).toString();
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

    public Response createObject(String objectName, String owner, long size) {
        return createObject(objectName, owner, MOCK_STORE_READ_ONLY, null, size);
    }

    public Response createObject(String objectName, String owner, String platform, String path, long size) {
        Client client = BossApplication.getClient();

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
