package de.buw.tmdt.plasma.services.sms.modeling.database;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false" // no eureka for this run
})
public class ModelingPersistenceTest {

    @Autowired
    ModelingRepository modelingRepository;

    @Transactional
    public void testCreateModeling() {
        Modeling dsm = new Modeling();

        SyntaxNode root = new ObjectNode("root");

        CombinedModel combinedModel = new CombinedModel("test", new SyntaxModel(
                root.getUuid(),
                new ArrayList<>(Collections.singletonList(root)),
                new ArrayList<>()),
                null
        );
        int numModels = 1000;
        for (int i = 0; i < numModels; i++) {
            combinedModel = combinedModel.copy();
            Class ec = new Class("blub" + i, "", "plasma:concept" + i);
            combinedModel.getProvisionalElements().add(ec);
            dsm.pushCombinedModel(combinedModel);
        }

        modelingRepository.save(dsm);

        Modeling model = modelingRepository.findById(dsm.getId()).orElse(null);

        assertNotNull(model);
        assertEquals(numModels, model.getAllCombinedModels().size());
        assertNotNull(model.getCurrentModel());
        assertIterableEquals(dsm.getAllCombinedModels(), model.getAllCombinedModels());

    }
}
