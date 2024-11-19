package de.buw.tmdt.plasma.services.sms.modeling;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.services.kgs.TestApplication;
import de.buw.tmdt.plasma.services.kgs.core.SemanticModelService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Disabled("Not suited for execution due to mismatch in ontology handling")
@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SemanticModelServiceTest {

    @Autowired
    private SemanticModelService testee;

    private final AdvancedCombinedModelGenerator generator = new AdvancedCombinedModelGenerator();

    @Test
    public void testAddSemanticModel() {
        generator.setMapStaffInstance(true);
        CombinedModel combinedModel = generator.getFlightModel();
        combinedModel.getSemanticModel().setId(UUID.randomUUID().toString().substring(0, 6));
        SemanticModel semanticModel = combinedModel.getSemanticModel();
        testee.createSemanticModel(semanticModel);

        SemanticModel restoredSM = testee.getSemanticModel(combinedModel.getSemanticModel().getId());

        //assertEquals(semanticModel, restoredSM, "Restored model does not match stored one.");
        testee.deleteSemanticModel(restoredSM.getId());
    }
}
