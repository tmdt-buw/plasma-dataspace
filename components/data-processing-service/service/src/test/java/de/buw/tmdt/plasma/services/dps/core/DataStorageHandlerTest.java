package de.buw.tmdt.plasma.services.dps.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.services.dps.api.SampleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataStorageHandlerTest {
    @TempDir
    Path directory;
    private DataStorageHandler storageHandler;

    @BeforeEach
    void setup() {
        storageHandler = new DataStorageHandler();
        storageHandler.storageDirectoryString = directory.toString();
        storageHandler.initStorage();
    }

    private MockMultipartFile flightFile() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/flight.json")) {
            assertNotNull(input);
            return new MockMultipartFile("flight.json", "flight.json", "application/json", input);
        }
    }

    @Test
    void uploadPreservesJsonContent() throws IOException {
        MockMultipartFile file = flightFile();
        SampleDTO sample = storageHandler.storeFile(null, file);
        assertNotNull(sample.getDataId());
        List<File> files = storageHandler.listFiles(sample.getDataId());
        assertEquals(1, files.size());
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(file.getBytes()), mapper.readTree(files.get(0)));
    }

    @Test
    void subsequentUploadPreservesPreviousVersion() throws IOException {
        MockMultipartFile file = flightFile();
        SampleDTO first = storageHandler.storeFile(null, file);
        File original = storageHandler.listFiles(first.getDataId()).get(0);
        byte[] originalBytes = Files.readAllBytes(original.toPath());
        SampleDTO second = storageHandler.storeFile(first.getDataId(), file);
        assertEquals(first.getDataId(), second.getDataId());
        List<File> files = storageHandler.listFiles(first.getDataId());
        assertEquals(2, files.size());
        assertTrue(files.contains(original));
        assertArrayEquals(originalBytes, Files.readAllBytes(original.toPath()));
    }
}
