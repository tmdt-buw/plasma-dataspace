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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class DataProcessingHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingHandlerTest.class);

    private DataProcessingHandler processingHandler;
    private DataStorageHandler storageHandler;

    @BeforeEach
    public void setup(@Mock OntologyApiClient ontologyApiClient){
        Map<String,String> prefixes = new HashMap<>();
        prefixes.put("local", CombinedModelGenerator.namespace);
        prefixes.put("plsm", "http://plasma.uni-wuppertal.de/sm/");
        prefixes.put("plcm", "http://plasma.uni-wuppertal.de/cm#");
        prefixes.put("plasma", "http://plasma.uni-wuppertal.de/");
        Mockito.lenient().when(ontologyApiClient.getPrefixes()).thenReturn(prefixes);
        storageHandler = new DataStorageHandler();
        processingHandler = new DataProcessingHandler(ontologyApiClient, storageHandler);
    }

    @Test
    void processFile() throws IOException {
        InputStream resourceAsStream = DataProcessingHandlerTest.class.getClassLoader().getResourceAsStream("flight.json");
        assertNotNull(resourceAsStream);
        MultipartFile mpf = new MockMultipartFile("flight.json", "flight.json", "application/json", resourceAsStream);

        SampleDTO sample = storageHandler.storeFile(null, mpf);
        String dataId = sample.getDataId();
        assertNotNull(dataId);
        log.info("DataId: {}", dataId);

        CombinedModel flightModel = CombinedModelGenerator.getFlightModel();
        String result = processingHandler.processFile(flightModel, dataId, null);
        assertEquals("abc",result , "Expected result for dataId " + dataId + " did not match");
    }
}