package org.genomebridge.boss.http;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class S3AmplidataTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));


    @Override
    public DropwizardAppRule<BossConfiguration> rule() {
        return RULE;
    }

    private AmazonS3 getClient() {
        return RULE.getConfiguration().getObjectStoreConfiguration().createClient();
    }

    //@Test
    // Taking the test annotation off of this method, since it (a) depends on S3 access, and
    // (b) has some finicky issues with clock skew.  We need to rewrite this as an integration
    // test on teh deployed system.
    public void testPresignedUrl() throws InterruptedException, URISyntaxException, IOException {
        String broadBucket = "genomebridgesparkci", broadKey = "test.txt";
        String bucket = "genomebridge-variantstore-ci";
        String key = "config/flannick-config.txt";

        AmazonS3 client = getClient();

        URL url = client.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET)
                .withExpiration(new Date(System.currentTimeMillis() + 2000)));

        assertEquals( 200, new DefaultHttpClient().execute(new HttpGet(url.toURI())).getStatusLine().getStatusCode() );

        Thread.sleep(3000);

        assertEquals( 403, new DefaultHttpClient().execute(new HttpGet(url.toURI())).getStatusLine().getStatusCode() );
    }
}
