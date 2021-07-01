package de.buw.tmdt.plasma.services.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {"plasma.config.local.path = C://temp"})
class VolumeConfigClientTest {

    @Value("${plasma.config.local.path}")
    protected String STORAGE_LOCATION;
    @Autowired
    VolumeConfigClient client;
    String configId = "testconfigId";

    @BeforeEach
    public void setUp() throws IOException {
        try {
            Files.delete(Path.of(STORAGE_LOCATION, configId + ".cfg"));
        } catch (NoSuchFileException ignore) {

        }
    }

    @Test
    void testGetConfig() {
        Map<String, Serializable> map = new HashMap<>();
        map.put("testvalue1", "testvalue1");
        ArrayList<String> list = new ArrayList<>(Arrays.asList("Test", " Test"));
        map.put("testvalue2", list);

        client.storeConfig(configId, map);

        Map<String, Serializable> config = client.getConfig(configId, null);
        assertEquals(map, config);
    }

    @Test
    void testStoreConfig() {

        Map<String, Serializable> map = new HashMap<>();
        map.put("testvalue1", "testvalue1");
        map.put("testvalue2", new ArrayList<>(Arrays.asList("Test", " Test")));

        client.storeConfig(configId, map);
    }

    @Test
    void testStoreConfigWithOverride() {

        Map<String, Serializable> map = new HashMap<>();
        map.put("testvalue1", "testvalue1");
        map.put("testvalue2", new ArrayList<>(Arrays.asList("Test", " Test")));

        client.storeConfig(configId, map);

        client.updateConfig(configId, map, null);
    }

    @Test
    void testStoreConfigWithoutOverride() {

        Map<String, Serializable> map = new HashMap<>();
        map.put("testvalue1", "testvalue1");
        map.put("testvalue2", new ArrayList<>(Arrays.asList("Test", " Test")));

        client.storeConfig(configId, map);

        assertThrows(UnsupportedOperationException.class, () -> client.storeConfig(configId, map));
    }

    @Test
    void testHasConfig() {
        assertFalse(client.hasConfig(configId));
        Map<String, Serializable> map = new HashMap<>();
        map.put("testvalue1", "testvalue1");
        map.put("testvalue2", new ArrayList<>(Arrays.asList("Test", " Test")));

        client.storeConfig(configId, map);
        assertTrue(client.hasConfig(configId));
    }
}