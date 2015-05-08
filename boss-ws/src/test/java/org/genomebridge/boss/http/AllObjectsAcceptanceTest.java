package org.genomebridge.boss.http;

import static org.fest.assertions.api.Assertions.assertThat;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.models.ResolveRequest;
import org.genomebridge.boss.http.models.ResolveResponse;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.net.HttpHeaders;

public class AllObjectsAcceptanceTest extends AbstractTest {

    public static int OK = Response.Status.OK.getStatusCode();
    public static int CREATED = Response.Status.CREATED.getStatusCode();
    public static int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    public static int GONE = Response.Status.GONE.getStatusCode();
    public static int CONFLICT = Response.Status.CONFLICT.getStatusCode();
    public static int FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    private Map<String,String> messages = BossApplication.getMessages();

    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testObjectStoreObjectCreation() {
        /**
         * "The user should be able to create a new ObjectDesc by POSTing to objects"
         */

        Client client = BossApplication.getClient();

        Response response = checkStatus(CREATED, createObject("Name", "tdanford", 500L));

        String location = checkHeader(response, HttpHeaders.LOCATION);

        response = check200( get(client, location) );

        ObjectDesc rec = response.readEntity(ObjectDesc.class);

        assertThat(rec).isNotNull();
        assertThat(rec.objectName).isEqualTo("Name");
        assertThat(rec.ownerId).isEqualTo("tdanford");
        assertThat(rec.sizeEstimateBytes).isEqualTo(500L);
    }

    @Test
    public void testNullNameObjectCreation() {
        /**
         * "The user should not be able to create an object with a null objectName"
         */
        ObjectDesc rec = fixture();
        rec.objectName = null;
        Response response = checkStatus(BAD_REQUEST, post(BossApplication.getClient(), objectsPath(), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(messages.get("objectValidation")+'.');
    }

    @Test
    public void testNullStoragePlatformObjectCreation() {
        /**
         * "The user should not be able to create an object with a null storagePlatform"
         */
        ObjectDesc rec = fixture();
        rec.storagePlatform = null;
        Response response = checkStatus(BAD_REQUEST, post(BossApplication.getClient(), objectsPath(), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(messages.get("storagePlatformValidation")+'.');
    }

    @Test
    public void testBadStoragePlatformObjectCreation() throws Exception {
        /**
         * "The user should not be able to create an object with a bogus storagePlatform"
         */
        ObjectDesc rec = fixture();
        rec.storagePlatform = "xyzzy";
        Response response = checkStatus(BAD_REQUEST, post(BossApplication.getClient(), objectsPath(), rec));
        StringBuffer objectStoreNames = new StringBuffer();
        for(String objectStore : BossApplication.getgObjectStores().keySet()){
    		objectStoreNames
    		.append(objectStore)
    		.append(", ");
    	}
    	objectStoreNames.append(OPAQUEURI);
       
        assertThat(response.readEntity(String.class)).isEqualTo(String.format(messages.get("storagePlatformOptions"),
        		objectStoreNames.append('.')));
    }

    @Test
    public void testNoPathFilesystemObjectCreation() {
        /**
         * "The user should not be able to create an opaqueURI object without a directoryPath"
         */
        ObjectDesc rec = fixture();
        rec.directoryPath = null;
        Response response = checkStatus(BAD_REQUEST, post(BossApplication.getClient(), objectsPath(), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(String.format(messages.get("directoryPathToSupply"),"opaqueURI") + '.');
    }

    @Test
    public void testNullOwnerObjectCreation() {
        /**
         * "The user should not be able to create an object with a null ownerId"
         */
        ObjectDesc rec = fixture();
        rec.ownerId = null;
        Response response = checkStatus(BAD_REQUEST, post(BossApplication.getClient(), objectsPath(), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(messages.get("ownerIdValidation")+'.');
    }

    @Test
    public void testBackdoorObjectCreation() {
        /**
         * "The user should not be able to create an object via an update"
         */
        ObjectDesc rec = fixture();
        Client client = BossApplication.getClient();
        final String fakeObjectId = "xyzzy";

        Response response = checkStatus(NOT_FOUND, post(client, String.format("%s/%s", objectsPath(), fakeObjectId), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(String.format(messages.get("objectNotFound"), fakeObjectId));

        // check that this is also true for deleted objects

        // until we have a mock object store, calling delete on an objectstore-object will fail, because
        // the system will reach out to the real objectstore and attempt to delete it. We'll cover that test
        // in the end-to-end integration tests.
        response = checkStatus(CREATED, createObject("Name", "tdanford", OPAQUEURI, "file:///path/to/file", 500L));
        ObjectDesc created = response.readEntity(ObjectDesc.class);
        String objectPath = checkHeader(response, HttpHeaders.LOCATION);

        checkStatus(OK, get(client, objectPath));
        checkStatus(OK, delete(client, objectPath));
        response = checkStatus( GONE, post(client, String.format("%s/%s", objectsPath(), created.objectId), rec));
        assertThat(response.readEntity(String.class)).isEqualTo(String.format(String.format(messages.get("objectDeleted"), created.objectId)));
        response = checkStatus( GONE, get(client, objectPath));
        assertThat(response.readEntity(String.class)).isEqualTo(String.format(messages.get("objectDeleted"), created.objectId));
    }

    @Test
    public void testFindByName() {
        Client client = BossApplication.getClient();
        ObjectDesc rec = fixture();
        rec.objectName = UUID.randomUUID().toString();
        String queryURL = objectsPath()+"?name="+rec.objectName;

        // shouldn't be any objects with our unique name yet
        checkStatus(NOT_FOUND,get(client,queryURL,"me"));

        // make an object with our name, and see if we can find it
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        Response response = checkStatus(OK,get(client,queryURL,"me"));
        GenericType<List<ObjectDesc>> genTyp = new GenericType<List<ObjectDesc>>() {};
        List<ObjectDesc> recs = response.readEntity(genTyp);
        assertThat(recs.size()).isEqualTo(1);
        assertThat(recs.get(0).objectName).isEqualTo(rec.objectName);

        // make another one, and see that we find both
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.readEntity(genTyp);
        assertThat(recs.size()).isEqualTo(2);
        assertThat(recs.get(0).objectName).isEqualTo(rec.objectName);
        assertThat(recs.get(1).objectName).isEqualTo(rec.objectName);

        // make one we can't see due to permissions, and make sure we still find just two
        rec.readers = arraySet("him","her");
        checkStatus(CREATED,post(client,objectsPath(),"me",rec));
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.readEntity(genTyp);
        assertThat(recs.size()).isEqualTo(2);

        // delete one of them and make sure we find just one
        checkStatus(OK,delete(client,objectsPath()+"/"+recs.get(0).objectId,"me"));
        String expectedId = recs.get(1).objectId;
        response = checkStatus(OK,get(client,queryURL,"me"));
        recs = response.readEntity(genTyp);
        assertThat(recs.size()).isEqualTo(1);
        assertThat(recs.get(0).objectId).isEqualTo(expectedId);
    }

    @Test
    public void testForceLocation() {
        Client client = BossApplication.getClient();

        // create an object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "fred";
        obj.objectName = "john";
        obj.readers = new String[1];
        obj.readers[0] = obj.ownerId;
        obj.writers = obj.readers;
        obj.sizeEstimateBytes = 500L;
        obj.storagePlatform = MOCK_STORE;
        Response response = checkStatus(CREATED, post(client,objectsPath(),obj.ownerId,obj));
        String objectPath = checkHeader(response, HttpHeaders.LOCATION);
        String objectId = response.readEntity(ObjectDesc.class).objectId;

        // resolve the object for a PUT
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.PUT;
        req.validityPeriodSeconds = 120;
        req.contentType = MediaType.TEXT_PLAIN;
        response = checkStatus(OK, post(client,objectPath+"/resolve",obj.ownerId,req));
        String putURL = response.readEntity(ResolveResponse.class).objectUrl.toString();

        // delete the object (but the PUT URL is still valid)
        checkStatus(OK, delete(client, objectPath, obj.ownerId));

        // write some content to the PUT URL
        String putContent = "Some content.";
        response = checkStatus(OK, client.target(putURL).request().put(Entity.entity(putContent,MediaType.APPLICATION_OCTET_STREAM)));

        // do a forceLocation for the key where we shoved the content
        // we're relying on "inside" knowledge to extract the key from the PUT URL -- not ideal
        int idx = putURL.indexOf(objectId);
        obj.directoryPath = putURL.substring(idx,idx+49);
        obj.forceLocation = true;
        response = checkStatus(CREATED, post(client,objectsPath(),obj.ownerId,obj));
        objectPath = checkHeader(response, HttpHeaders.LOCATION);
        ObjectDesc rec = response.readEntity(ObjectDesc.class);
        assertThat(rec).isNotNull();
        assertThat(rec.directoryPath).isEqualTo(obj.directoryPath);

        checkStatus(OK, delete(client, objectPath, obj.ownerId));
    }
    
    @Test
    public void testForceLocationWithReadOnlyAccessForPUT() {
        Client client = BossApplication.getClient();

        // create an object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "fred";
        obj.objectName = "john";
        obj.readers = new String[1];
        obj.readers[0] = obj.ownerId;
        obj.writers = obj.readers;
        obj.sizeEstimateBytes = 500L;
        obj.storagePlatform = MOCK_STORE_READ_ONLY;
        Response response = checkStatus(CREATED, post(client,objectsPath(),obj.ownerId,obj));
        String objectPath = checkHeader(response, HttpHeaders.LOCATION);
        // resolve the object for a PUT
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.PUT;
        req.validityPeriodSeconds = 120;
        req.contentType = MediaType.TEXT_PLAIN;
        response = checkStatus(FORBIDDEN, post(client,objectPath+"/resolve",obj.ownerId,req));
       
    }

    @Test
    public void testForceLocationWithReadOnlyAccessForGET() {
        Client client = BossApplication.getClient();

        // create an object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "fred";
        obj.objectName = "john";
        obj.readers = new String[1];
        obj.readers[0] = obj.ownerId;
        obj.writers = obj.readers;
        obj.sizeEstimateBytes = 500L;
        obj.storagePlatform = MOCK_STORE_READ_ONLY;
        Response response = checkStatus(CREATED, post(client,objectsPath(),obj.ownerId,obj));
        String objectPath = checkHeader(response, HttpHeaders.LOCATION);
        // resolve the object for a PUT
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.GET;
        req.validityPeriodSeconds = 120;
        req.contentType = MediaType.TEXT_PLAIN;
        response = checkStatus(OK, post(client,objectPath+"/resolve",obj.ownerId,req));
       
    }
    
    @Test
    public void testForceLocationWithReadOnlyAccessForHEAD() {
        Client client = BossApplication.getClient();

        // create an object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "fred";
        obj.objectName = "john";
        obj.readers = new String[1];
        obj.readers[0] = obj.ownerId;
        obj.writers = obj.readers;
        obj.sizeEstimateBytes = 500L;
        obj.storagePlatform = MOCK_STORE_READ_ONLY;
        Response response = checkStatus(CREATED, post(client,objectsPath(),obj.ownerId,obj));
        String objectPath = checkHeader(response, HttpHeaders.LOCATION);
        // resolve the object for a PUT
        ResolveRequest req = new ResolveRequest();
        req.httpMethod = HttpMethod.HEAD;
        req.validityPeriodSeconds = 120;
        req.contentType = MediaType.TEXT_PLAIN;
        response = checkStatus(OK, post(client,objectPath+"/resolve",obj.ownerId,req));
       
    }


    @Test
    public void testForceBogusLocation() {
        // create an object
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "fred";
        obj.objectName = "john";
        obj.readers = new String[1];
        obj.readers[0] = obj.ownerId;
        obj.writers = obj.readers;
        obj.sizeEstimateBytes = 500L;
        obj.storagePlatform = MOCK_STORE_READ_ONLY;
        obj.directoryPath = "doesNotExist";
        obj.forceLocation = true;
        Response response = checkStatus(CONFLICT, post(BossApplication.getClient(),objectsPath(),obj.ownerId,obj));
        assertThat(response.readEntity(String.class)).isEqualTo(messages.get("noSuchLocation"));
    }
}
