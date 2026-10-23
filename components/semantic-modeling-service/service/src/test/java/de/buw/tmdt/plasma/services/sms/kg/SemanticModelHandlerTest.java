package de.buw.tmdt.plasma.services.sms.kg;

import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PLASMA_TEST_FUSEKI", matches = ".+")
class SemanticModelHandlerTest {
    @Test void storesQueriesAndDeletesOnlyTheRequestedModel() {
        String endpoint = System.getenv("PLASMA_TEST_FUSEKI");
        assertTrue(endpoint.contains("cleanup"), "Only an explicitly named disposable cleanup dataset may be used");
        var store = new SemanticModelManagement(endpoint);
        var generator = new AdvancedCombinedModelGenerator(); generator.setMapStaffInstance(true);
        var first = generator.getFlightModel().getSemanticModel();
        var second = new AdvancedCombinedModelGenerator().getFlightModel().getSemanticModel();
        try {
            var rdf = CMToRDFMapper.convertToRDFModel(first); store.persistModel(rdf);
            store.persistModel(CMToRDFMapper.convertToRDFModel(second));
            var restored = store.queryModel(first.getId());
            assertTrue(rdf.isIsomorphicWith(restored), () -> "Missing: " + rdf.difference(restored) + " Extra: " + restored.difference(rdf));
            store.deleteModel(first.getId());
            assertTrue(store.queryModel(first.getId()).isEmpty());
            assertFalse(store.queryModel(second.getId()).isEmpty());
        } finally { store.deleteModel(first.getId()); store.deleteModel(second.getId()); }
    }
}
