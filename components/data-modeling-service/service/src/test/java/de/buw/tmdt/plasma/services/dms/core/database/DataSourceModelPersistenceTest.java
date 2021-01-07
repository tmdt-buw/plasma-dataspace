package de.buw.tmdt.plasma.services.dms.core.database;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.repository.DataSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false" // no eureka for this run
})
public class DataSourceModelPersistenceTest {

    @Autowired
    DataSourceRepository dataSourceRepository;

    @Test
    @Transactional
    public void testCreateDataSourceModel() {
        DataSourceModel dsm = new DataSourceModel();

        SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

        CombinedModel combinedModel = new CombinedModel("test", new SyntaxModel(
                root.getUuid(),
                new ArrayList<>(Collections.singletonList(root)),
                new ArrayList<>()),
                null
        );
        int numModels = 1000;
        for (int i = 0; i < numModels; i++) {
            combinedModel = combinedModel.copy();
            EntityConcept ec = new EntityConcept("blub" + i, "", "plasma:ec" + i);
            combinedModel.getProvisionalEntityConcepts().add(ec);
            dsm.pushCombinedModel(combinedModel);
        }

        dataSourceRepository.save(dsm);

        DataSourceModel model = dataSourceRepository.findById(dsm.getUuid()).orElse(null);

        assertNotNull(model);
        assertEquals(numModels, model.getAllCombinedModels().size());
        assertNotNull(model.getCurrentModel());
        assertIterableEquals(dsm.getAllCombinedModels(), model.getAllCombinedModels());

    }
}
