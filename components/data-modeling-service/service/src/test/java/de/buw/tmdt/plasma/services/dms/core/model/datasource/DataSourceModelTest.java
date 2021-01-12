package de.buw.tmdt.plasma.services.dms.core.model.datasource;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test to simulate and control stack behaviour for {@link CombinedModel}s in the {@link DataSourceModel}.
 */
class DataSourceModelTest {

    DataSourceModel dataSourceModel;

    int counter = 1;
    int modelcounter = 1;

    @BeforeEach
    void setUp() {
        dataSourceModel = new DataSourceModel();
        modelcounter = 1;
        counter = 1;
    }

    @Test
    void getCurrentModel() {
        CombinedModel model = getCombinedModel();
        dataSourceModel.pushCombinedModel(model);
        assertEquals(model, dataSourceModel.getCurrentModel());
    }

    @Test
    void getCurrentModelOnEmptyStack() {
        assertThrows(UnsupportedOperationException.class, () -> dataSourceModel.getCurrentModel());
    }

    @Test
    void pushCombinedModel() {
        CombinedModel model;
        int count = 0;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        while (count < 10) {
            model = getCombinedModel();
            dataSourceModel.pushCombinedModel(model);
            count++;
            assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        }
    }

    @Test
    void popCombinedModel() {
        CombinedModel model;
        int count = 0;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        while (count < 10) {
            model = getCombinedModel();
            dataSourceModel.pushCombinedModel(model);
            count++;
            assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
        for (int i = 0; i < 3; i++) {
            dataSourceModel.popCombinedModel();
            count--;
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
    }

    @Test
    void popCombinedModelOnEmptyStack() {
        assertThrows(UnsupportedOperationException.class,
                () -> dataSourceModel.popCombinedModel());

        // we add a model and see if the pointer behaves naturally
        CombinedModel model = getCombinedModel();
        dataSourceModel.pushCombinedModel(model);

        assertEquals(model, dataSourceModel.getCurrentModel());

    }

    @Test
    void pushCombinedModelOnReducedStackPointer() {
        CombinedModel model;
        int count = 0;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        while (count < 10) {
            model = getCombinedModel();
            dataSourceModel.pushCombinedModel(model);
            count++;
            assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
        for (int i = 0; i < 3; i++) {
            dataSourceModel.popCombinedModel();
            count--;
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
        CombinedModel intermediateModel = getCombinedModel();

        dataSourceModel.pushCombinedModel(intermediateModel);
        count++;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        assertEquals(intermediateModel, dataSourceModel.getCurrentModel());

        // ensure we are actually a max stack position
        assertThrows(UnsupportedOperationException.class,
                () -> dataSourceModel.restoreCombinedModel());
    }


    @Test
    void restoreCombinedModel() {
        CombinedModel model;
        int count = 0;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        while (count < 10) {
            model = getCombinedModel();
            dataSourceModel.pushCombinedModel(model);
            count++;
            assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
        for (int i = 0; i < 3; i++) {
            dataSourceModel.popCombinedModel();
            count--;
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
        for (int i = 0; i < 3; i++) {
            dataSourceModel.restoreCombinedModel();
            count++;
        }
        assertEquals("model-" + count, dataSourceModel.getCurrentModel().getDataSourceUuid());
    }

    @Test
    void restoreCombinedModelOnMaxStackPointer() {
        CombinedModel model;
        int count = 0;
        assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        while (count < 10) {
            model = getCombinedModel();
            dataSourceModel.pushCombinedModel(model);
            count++;
            assertEquals(count, dataSourceModel.getAllCombinedModels().size());
        }
        assertThrows(UnsupportedOperationException.class, () -> dataSourceModel.restoreCombinedModel());
    }


    private CombinedModel getCombinedModel() {
        SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

        SchemaNode node = new PrimitiveNode(
                "node-" + counter++,
                DataType.Unknown
        );
        Edge edge = new Edge(root.getUuid(), node.getUuid());
        SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), new ArrayList<>(Arrays.asList(root, node)), new ArrayList<>(Collections.singletonList(edge)));

        EntityType entityType = new EntityType(
                "type-" + counter,
                "description",
                new EntityConcept("ECName", "ECDescription", "plasma:concept" + counter++)
        );
        SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());

        return new CombinedModel("model-" + modelcounter++, syntaxModel, semanticModel);
    }
}