package org.infinispan.server.test.client.rest;

import static org.infinispan.server.test.client.rest.RESTHelper.KEY_A;
import static org.infinispan.server.test.client.rest.RESTHelper.KEY_B;
import static org.infinispan.server.test.client.rest.RESTHelper.KEY_C;
import static org.infinispan.server.test.client.rest.RESTHelper.delete;
import static org.infinispan.server.test.client.rest.RESTHelper.fullPathKey;
import static org.infinispan.server.test.client.rest.RESTHelper.get;
import static org.infinispan.server.test.client.rest.RESTHelper.head;
import static org.infinispan.server.test.client.rest.RESTHelper.post;
import static org.infinispan.server.test.client.rest.RESTHelper.put;
import static org.infinispan.server.test.util.ITestUtils.isReplicatedMode;
import static org.infinispan.server.test.util.ITestUtils.sleepForSecs;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the RESTLocal client.
 *
 * @author <a href="mailto:jvilkola@redhat.com">Jozef Vilkolak</a>
 * @author <a href="mailto:mlinhard@redhat.com">Michal Linhard</a>
 * @author mgencur
 */
public abstract class AbstractRESTClusteredIT {

    protected abstract int getRestPort1();
    protected abstract int getRestPort2();

    protected abstract List<RemoteInfinispanServer> getServers();

    @Before
    public void setUp() throws Exception {
        if (isReplicatedMode()) {
            RESTHelper.addServer(getServers().get(0).getRESTEndpoint().getInetAddress().getHostName(), getRestPort1(), getServers().get(0).getRESTEndpoint().getContextPath());
            RESTHelper.addServer(getServers().get(1).getRESTEndpoint().getInetAddress().getHostName(), getRestPort2(), getServers().get(1).getRESTEndpoint().getContextPath());
        } else {
            RESTHelper.addServer(getServers().get(0).getRESTEndpoint().getInetAddress().getHostName(), getServers().get(0).getRESTEndpoint().getContextPath());
            RESTHelper.addServer(getServers().get(1).getRESTEndpoint().getInetAddress().getHostName(), getServers().get(1).getRESTEndpoint().getContextPath());
        }

        delete(fullPathKey(KEY_A));
        delete(fullPathKey(KEY_B));
        delete(fullPathKey(KEY_C));

        head(fullPathKey(KEY_A), HttpServletResponse.SC_NOT_FOUND);
        head(fullPathKey(KEY_B), HttpServletResponse.SC_NOT_FOUND);
        head(fullPathKey(KEY_C), HttpServletResponse.SC_NOT_FOUND);
    }

    @After
    public void tearDown() throws Exception {
        delete(fullPathKey(KEY_A));
        delete(fullPathKey(KEY_B));
        delete(fullPathKey(KEY_C));
        RESTHelper.clearServers();
    }

    @Test
    public void testReplicationPut() throws Exception {
        put(fullPathKey(0, KEY_A), "data", "text/plain");
        get(fullPathKey(1, KEY_A), "data");
    }

    @Test
    public void testReplicationPost() throws Exception {
        post(fullPathKey(0, KEY_A), "data", "text/plain");
        get(fullPathKey(1, KEY_A), "data");
    }

    @Test
    public void testReplicationDelete() throws Exception {
        post(fullPathKey(0, KEY_A), "data", "text/plain");
        get(fullPathKey(1, KEY_A), "data");
        delete(fullPathKey(0, KEY_A));
        head(fullPathKey(1, KEY_A), HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testReplicationWipeCache() throws Exception {
        post(fullPathKey(0, KEY_A), "data", "text/plain");
        post(fullPathKey(0, KEY_B), "data", "text/plain");
        head(fullPathKey(0, KEY_A));
        head(fullPathKey(0, KEY_B));
        delete(fullPathKey(0, null));
        head(fullPathKey(1, KEY_A), HttpServletResponse.SC_NOT_FOUND);
        head(fullPathKey(1, KEY_B), HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testReplicationTTL() throws Exception {
        post(fullPathKey(0, KEY_A), "data", "application/text", HttpServletResponse.SC_OK,
                // headers
                "Content-Type", "application/text", "timeToLiveSeconds", "2");
        head(fullPathKey(1, KEY_A));
        sleepForSecs(2.1);
        // should be evicted
        head(fullPathKey(1, KEY_A), HttpServletResponse.SC_NOT_FOUND);
    }
}
