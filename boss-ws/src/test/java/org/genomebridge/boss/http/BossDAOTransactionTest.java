/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.genomebridge.boss.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.boss.http.db.BossDAO;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created: 1/22/15
 *
 * @author <a href="mailto:grushton@broadinstitute.org">grushton</a>
 */
public class BossDAOTransactionTest extends ResourcedTest {


    @ClassRule
    public static final DropwizardAppRule<BossConfiguration> RULE =
            new DropwizardAppRule<>(BossApplication.class,
                    resourceFilePath("boss-config.yml"));

    private static BossDAO dao1 = null;
    private static BossDAO dao2 = null;

    @BeforeClass
    public static void setup() throws Exception {
        dao1 = BossApplication.getDAO(RULE.getConfiguration(), RULE.getEnvironment());
        dao2 = BossApplication.getDAO(RULE.getConfiguration(), RULE.getEnvironment());
    }

    /**
     * This test ensures that transactional queries are instance and thread-safe.
     * By starting a transaction in one thread and making other calls to the same database
     * row in other threads, we can check to see that other calls are blocked on the row in
     * question.
     */
    @Test
    public void testTransactional() {
        String user = "testUser";
        String id = UUID.randomUUID().toString();

        dao1.begin();
        dao1.insertReaders(id, Collections.singletonList(user));
        assertThat(dao1.findReadersById(id).contains(user));

        // This will time out because da01 is in a transaction.
        // If it does return results, that will be returned as FALSE
        assertThat(callFindReadersByIdWithTimeout(dao2, id).equals(Boolean.TRUE));

        dao1.commit();
        assertThat(dao2.findReadersById(id).contains(user));

    }

    /**
     * Returns true if the list is empty or it times out.
     * Return false if the dao call has any results.
     *
     * @param dao BossDAO to run method with
     * @param id Reader id
     * @return Boolean
     */
    private Object callFindReadersByIdWithTimeout(final BossDAO dao, final String id) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<Object> task = new Callable<Object>() {
            public Object call() {
                return dao.findReadersById(id).isEmpty();
            }
        };
        Future<Object> future = executor.submit(task);
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
            return true;
        }
    }

}
