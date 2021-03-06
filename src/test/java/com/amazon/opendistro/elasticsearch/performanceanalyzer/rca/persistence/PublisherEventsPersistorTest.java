package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.decisionmaker.actions.Action;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.decisionmaker.actions.ImpactVector;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PublisherEventsPersistorTest {
    private Path testLocation = null;
    private final String baseFilename = "rca.test.file";

    private Persistable persistable;
    private PublisherEventsPersistor publisherEventsPersistor;

    @Before
    public void init() throws Exception {
        String cwd = System.getProperty("user.dir");
        testLocation = Paths.get(cwd, "src", "test", "resources", "tmp", "file_rotate");
        Files.createDirectories(testLocation);
        FileUtils.cleanDirectory(testLocation.toFile());

        persistable = new SQLitePersistor(
                testLocation.toString(), baseFilename, String.valueOf(1), TimeUnit.SECONDS, 1);
        publisherEventsPersistor = new PublisherEventsPersistor(persistable);
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.cleanDirectory(testLocation.toFile());
    }

    @Test
    public void actionPublished() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final MockAction mockAction = new MockAction();

        publisherEventsPersistor.persistAction(mockAction);

        PersistedAction actionsSummary = persistable.read(PersistedAction.class);
        Assert.assertNotNull(actionsSummary);
        Assert.assertEquals(actionsSummary.getActionName(), mockAction.name());
        Assert.assertEquals(actionsSummary.getNodeIds(), "{1,2}");
        Assert.assertEquals(actionsSummary.getNodeIps(), "{1.1.1.1,2.2.2.2}");
        Assert.assertEquals(actionsSummary.isActionable(), mockAction.isActionable());
        Assert.assertEquals(actionsSummary.getCoolOffPeriod(), mockAction.coolOffPeriodInMillis());
        Assert.assertEquals(actionsSummary.isMuted(), mockAction.isMuted());
        Assert.assertEquals(actionsSummary.getSummary(), mockAction.summary());
    }

    public class MockAction implements Action {

        @Override
        public boolean isActionable() {
            return false;
        }

        @Override
        public long coolOffPeriodInMillis() {
            return 0;
        }

        @Override
        public List<NodeKey> impactedNodes() {
            List<NodeKey> nodeKeys = new ArrayList<>();
            nodeKeys.add(new NodeKey(new InstanceDetails.Id("1"), new InstanceDetails.Ip("1.1.1.1")));
            nodeKeys.add(new NodeKey(new InstanceDetails.Id("2"), new InstanceDetails.Ip("2.2.2.2")));
            return nodeKeys;
        }

        @Override
        public Map<NodeKey, ImpactVector> impact() {
            return null;
        }

        @Override
        public String name() {
            return "MockAction";
        }

        @Override
        public String summary() {
            return "MockSummary";
        }

        @Override
        public boolean isMuted() {
            return false;
        }
    }
}
