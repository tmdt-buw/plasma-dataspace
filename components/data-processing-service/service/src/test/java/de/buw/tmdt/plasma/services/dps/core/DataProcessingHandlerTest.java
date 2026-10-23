package de.buw.tmdt.plasma.services.dps.core;

import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import java.io.StringReader;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DataProcessingHandlerTest {
    @TempDir Path directory;
    DataStorageHandler storage;
    DataProcessingHandler handler;
    @BeforeEach void setup() {
        storage = new DataStorageHandler(); storage.storageDirectoryString = directory.toString(); storage.initStorage();
        handler = new DataProcessingHandler(storage);
        ReflectionTestUtils.setField(handler, "webClient", WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body("{}").build())).build());
    }
    @Test void processesAllRecordsWithoutChangingTheModelOrOriginalFile() throws Exception {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream("/flight.json")) { assertNotNull(input); bytes = input.readAllBytes(); }
        var sample = storage.storeFile(null, new MockMultipartFile("file", "flight.json", "application/json", bytes));
        var model = new AdvancedCombinedModelGenerator().getFlightModel(); String id = model.getSemanticModel().getId();
        String turtle = handler.processFile(model, sample.getDataId(), null);
        var result = ModelFactory.createDefaultModel().read(new StringReader(turtle), null, "TURTLE");
        assertEquals(2, result.listSubjectsWithProperty(RDF.type, result.createResource(AdvancedCombinedModelGenerator.namespace + "flight")).toList().size());
        assertTrue(result.contains(null, result.createProperty(AdvancedCombinedModelGenerator.namespace + "identifier"), "AL0025-20201123-1456-1734"));
        assertEquals(id, model.getSemanticModel().getId());
        assertArrayEquals(bytes, java.nio.file.Files.readAllBytes(storage.listFiles(sample.getDataId()).get(0).toPath()));
    }
    @Test void unknownDataIsReportedAsNotFound() {
        var error = assertThrows(ResponseStatusException.class, () -> handler.processFile(new AdvancedCombinedModelGenerator().getFlightModel(), "missing", null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
