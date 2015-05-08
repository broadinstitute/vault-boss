package org.genomebridge.boss.http;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;
import io.dropwizard.testing.junit.DropwizardAppRule;

import java.net.URI;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.genomebridge.boss.http.models.ObjectDesc;
import org.genomebridge.boss.http.models.ResolveRequest;
import org.genomebridge.boss.http.models.ResolveResponse;
import org.genomebridge.boss.http.objectstore.ObjectStoreConfiguration;
import org.genomebridge.boss.http.service.BossAPI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DatabaseBossAPITest extends ResourcedTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    private static BossAPI api = null;
    
    public static int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    
    @BeforeClass
    public static void setup() {
        api = BossApplication.getAPI();
    }

    @Test
    public void testUpdateAndRetrieveObject() {
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "tdanford";
        obj.sizeEstimateBytes = 1000L;
        obj.objectName = "Test Name";
        obj.readers = new String[] { "tdanford", "testuser", "tdanford" };
        obj.writers = new String[] { "carlyeks", "tdanford", "testuser", "carlyeks" };
        obj.storagePlatform = MOCK_STORE_READ_ONLY;

        assertThat(api.insertObject(obj,"remoteUser")).isNull();

        ObjectDesc retrieved = new ObjectDesc();
        assertThat(api.getObject(obj.objectId,"testuser",retrieved)).isNull();

        assertThat(retrieved.objectId).isEqualTo(obj.objectId);
        assertThat(retrieved.ownerId).isEqualTo(obj.ownerId);
        assertThat(retrieved.objectName).isEqualTo(obj.objectName);
        assertThat(retrieved.sizeEstimateBytes).isEqualTo(obj.sizeEstimateBytes);
        assertThat(retrieved.storagePlatform).isEqualTo(obj.storagePlatform);
        assertThat(retrieved.readers).containsOnly("tdanford", "testuser");
        assertThat(retrieved.writers).containsOnly("tdanford", "carlyeks", "testuser");
    }

    @Test
    public void testGeneratePresignedURL() {
        testGeneratePresignedURL(null, null);
    }

    @Test
    public void testGeneratePresignedURLWithContent() {
        testGeneratePresignedURL("application/octet-stream", "deadf00dbeef1234567890abcdefdead");
    }

    private void testGeneratePresignedURL(String contentType, String contentMD5) {
        ObjectDesc obj = new ObjectDesc();
        obj.ownerId = "tdanford";
        obj.sizeEstimateBytes = 1000L;
        obj.objectName = "Test Name";
        obj.readers = new String[] { "tdanford", "testuser" };
        obj.writers = new String[] { "carlyeks", "tdanford", "testuser" };
        obj.storagePlatform = MOCK_STORE_READ_ONLY;

        assertThat(api.insertObject(obj,"remoteUser")).isNull();

        try {
            ResolveRequest req = new ResolveRequest();
            req.httpMethod = HttpMethod.GET;
            req.validityPeriodSeconds = 10;
            req.contentType = contentType;
            req.contentMD5Hex = contentMD5;
            ResolveResponse resp = new ResolveResponse();
            assertThat(api.resolveObject(obj.objectId,"tdanford",req,resp)).isNull();
            URI uri = resp.objectUrl;
            assertThat(uri).isNotNull();
            ObjectStoreConfiguration config = RULE.getConfiguration().getObjectStores().get(obj.storagePlatform);
            String urlToExpect = config.endpoint + '/' + config.bucket + '/' + obj.objectId;
            assertThat(uri.toString()).startsWith(urlToExpect);
        }
        catch (NullPointerException e) {
            // If the user doesn't have a correct objectstore configuration, this is a typical symptom
            fail(BossApplication.getMessages().get("serverError"), e);
        }
    }
    

}
