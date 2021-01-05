package de.buw.tmdt.plasma.services.dms.core.database;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.repository.DataSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class MondoDBTest {

    @Autowired
    DataSourceRepository dataSourceRepository;

    @Test
    public void testCreateDataSourceModel() {
        DataSourceModel dsm = new DataSourceModel();

        SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

        CombinedModel combinedModel = new CombinedModel("test", new SyntaxModel(
                root.getUuid(),
                new ArrayList<>(Collections.singletonList(root)),
                new ArrayList<>()),
                null
        );

        dsm.pushCombinedModel(combinedModel);

        dataSourceRepository.save(dsm);

        DataSourceModel model = dataSourceRepository.findById(dsm.getUuid()).orElse(null);

        assertNotNull(model);
        assertEquals(1, model.getAllCombinedModels().size());
        assertNotNull(model.getCurrentModel());
    }
}
