package com.hazelcast.map.impl.mapstore;

import com.google.common.collect.Lists;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapLoader;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.config.MapStoreConfig.InitialLoadMode.LAZY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class MapLoaderUnexpectedLoadedItemsTest extends HazelcastTestSupport {

    private static final boolean LOAD_ALL_KEYS = false;
    private static final boolean LOAD_PROVIDED_KEYS = true;
    private static final Integer[] KEYS_TO_LOAD = {0, 1, 2, 3, 4, 5};

    @Test
    public void loadAllAbortsIfItemFromOtherPartitionIsLoaded() {
        String mapName = randomString();
        DummyMapLoader mapLoader = new DummyMapLoader(LOAD_ALL_KEYS);
        Config config = getConfig(mapName, mapLoader);

        HazelcastInstance instance = createHazelcastInstance(config);
        IMap<?, ?> map = instance.getMap(mapName);
        map.clear();

        map.loadAll(true);

        assertThat(map.size(), lessThan(KEYS_TO_LOAD.length));

        instance.shutdown();
    }

    @Test
    public void loadAllLoadsAllIfProvidedKeysLoadedFromStore() {
        String mapName = randomString();
        DummyMapLoader mapLoader = new DummyMapLoader(LOAD_PROVIDED_KEYS);
        Config config = getConfig(mapName, mapLoader);

        HazelcastInstance instance = createHazelcastInstance(config);
        IMap<?, ?> map = instance.getMap(mapName);
        map.clear();

        map.loadAll(true);

        assertThat(map.size(), equalTo(KEYS_TO_LOAD.length));

        instance.shutdown();
    }

    private Config getConfig(String mapName1, DummyMapLoader mapLoader) {
        Config config = getConfig();
        config.getMapConfig(mapName1).getMapStoreConfig()
              .setEnabled(true)
              .setInitialLoadMode(LAZY)
              .setImplementation(mapLoader);
        return config;
    }

    public static class DummyMapLoader implements MapLoader<Integer, String> {

        private final boolean useProvidedKeys;

        private DummyMapLoader(boolean useProvidedKeys) {
            this.useProvidedKeys = useProvidedKeys;
        }

        public String load(Integer key) {
            return key.toString();
        }

        public Map<Integer, String> loadAll(Collection<Integer> keys) {
            Map<Integer, String> map = new HashMap<Integer, String>();

            for (Integer key : getKeysToLoad(keys)) {
                map.put(key, load(key));
            }

            return map;
        }

        private Iterable<Integer> getKeysToLoad(Collection<Integer> keys) {
            if (useProvidedKeys) {
                return keys;
            }
            return loadAllKeys();
        }

        public Iterable<Integer> loadAllKeys() {
            return Lists.newArrayList(KEYS_TO_LOAD);
        }
    }

}
