package de.buw.tmdt.plasma.services.dps.core;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.services.dps.CombinedModelGenerator;
import de.buw.tmdt.plasma.services.dps.api.SampleDTO;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.OntologyApiClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DataStorageHandlerTest {

    static DataStorageHandler storageHandler;

    private static String dataId;

    @BeforeAll
    public static void setup(){
        storageHandler = new DataStorageHandler();
    }

    @Test
    @Order(1)
    void uploadFile() throws IOException {
        InputStream resourceAsStream = DataStorageHandlerTest.class.getClassLoader().getResourceAsStream("flight.json");
        assertNotNull(resourceAsStream);
        MultipartFile mpf = new MockMultipartFile("flight.json", "flight.json", "application/json", resourceAsStream);

        SampleDTO sample = storageHandler.storeFile(null, mpf);
        dataId = sample.getDataId();
        assertNotNull(dataId);
    }

    @Test
    @Order(2)
    void listFiles() {
        List<File> files = storageHandler.listFiles(dataId);
        assertTrue(files.size() > 0);
    }
}