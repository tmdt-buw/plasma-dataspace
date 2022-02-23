package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Instance;
import de.buw.tmdt.plasma.datamodel.semanticmodel.ObjectProperty;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static de.buw.tmdt.plasma.datamodel.CombinedModelGenerator.getCombinedModel;
import static org.junit.jupiter.api.Assertions.*;

public class CombinedModelSemanticModificationTest {

    @Test
    public void testApplyFailsIfFinalized() {
        CombinedModel model = getCombinedModel();
        model.finalizeModel();

        Class companyConcept = new Class("plasma:company", "company", "A company");
        Instance companyType = new Instance(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        assertThrows(UnsupportedOperationException.class, () -> model.apply(modification));
    }

    @Test
    public void testApplyAddOneEntity() {
        CombinedModel model = getCombinedModel();

        Class companyConcept = new Class("plasma:company", "company", "A company");
        Instance companyType = new Instance(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyType), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    @Test
    public void testApplyAddOneEntityAndOneRelation() {
        CombinedModel model = getCombinedModel();

        Instance airlineType = model.getSemanticModel().getNodes().stream().filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(instance -> instance.getSemanticClass().getURI().equals("plasma:airline")).findFirst().orElseThrow();

        Class companyConcept = new Class("plasma:company", "company", "A company");
        Instance companyType = new Instance(companyConcept.getLabel(), "", companyConcept);

        Relation airlineCompanyRelation = new ObjectProperty(airlineType.getUuid(), companyType.getUuid(), "plasma:isA");

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntityAndOneRelation", Collections.singleton(companyType), Collections.singleton(airlineCompanyRelation), null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyType), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size() + 1, model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
        assertTrue(model.getSemanticModel().getEdges().contains(airlineCompanyRelation), "New relation is not available");
    }

    @Test
    public void testApplyReplaceOneEntity() {
        CombinedModel model = getCombinedModel();

        Instance iataCodeType = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(instance -> instance.getSemanticClass().getURI().equals("plasma:iata_code")).findFirst().orElseThrow();

        Class codeConcept = new Class("plasma:code", "code", "A sort of identifier or short version of text");
        Instance codeType = new Instance(iataCodeType.getUuid(),
                iataCodeType.getURI(),
                "code",
                iataCodeType.getSyntaxNodeUuid(),
                iataCodeType.getOriginalLabel(),
                "The code of this airline",
                codeConcept,
                iataCodeType.getXCoordinate(),
                iataCodeType.getYCoordinate());

        DeltaModification modification = new DeltaModification("mod_testApplyReplaceOneEntity", Collections.singleton(codeType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        Instance replacedEntity = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(Instance -> Objects.equals(Instance.getUuid(), codeType.getUuid())).findFirst().orElseThrow();
        assertEquals(replacedEntity.getUuid(), iataCodeType.getUuid(), "Replacement type does not have the same id as replaced entity");
        assertEquals(codeConcept.getURI(), replacedEntity.getSemanticClass().getURI(), "New entity concept is not assigned");
        assertEquals(codeType.getDescription(), replacedEntity.getDescription(), "New entity description is not applied correctly");
        assertTrue(model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .noneMatch(instance -> instance.getLabel().equals(iataCodeType.getLabel())), "Old entity type is not completely replaced");
        assertTrue(model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .noneMatch(instance -> instance.getSemanticClass().getURI().equals(iataCodeType.getSemanticClass().getURI())), "Old entity concept is still lingering");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    @Test
    public void testApplyMapOneEntityToPrimitiveNode() {
        CombinedModel model = getCombinedModel();

        // get the target node we want to map to
        SchemaNode timeNode = model.getSyntaxModel().getNodes().stream().filter(schemaNode -> schemaNode.getLabel().equals("time")).findFirst().orElseThrow();

        // check that the node is not already mapped for some reason
        assertTrue(timeNode instanceof PrimitiveNode);
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getPrimitiveUUID().equals(timeNode.getUuid())));
        assertTrue(model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .noneMatch(node -> Objects.equals(node.getSyntaxNodeUuid(), timeNode.getUuid())));

        // create the new type and concept
        Class timeConcept = new Class("plasma:time", "time", "A time");
        Instance timeType = new Instance(timeConcept.getLabel(), "", timeConcept);
        timeType.setSyntaxNodeUuid(timeNode.getUuid());

        DeltaModification modification = new DeltaModification("mod_testApplyMapOneEntityToSPrimitiveNode", Collections.singleton(timeType), null, null, null, 0.0);

        model.apply(modification);

        // check numbers
        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // check entity properties
        Instance addedTimeEntity = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(Instance -> Objects.equals(Instance.getUuid(), timeType.getUuid())).findFirst().orElseThrow();
        assertEquals(timeType.getUuid(), addedTimeEntity.getUuid(), "Added type does not have the same id as added entity");
        assertEquals(timeNode.getLabel(), addedTimeEntity.getOriginalLabel(), "New entity type's original label is not set properly");
        assertEquals(timeNode.getUuid(), addedTimeEntity.getSyntaxNodeUuid(), "New entity type's mapping uuid is not set properly");

        // ensure that the explicit mapping from entity to syntax has been created
        assertTrue(model.generatePrimitiveInstanceMappings().stream().anyMatch(mapping -> mapping.getInstanceId().equals(addedTimeEntity.getUuid())));
    }

    @Test
    public void testApplyMapOneEntityToNonPrimitiveNode() {
        CombinedModel model = getCombinedModel();

        // get the target node we want to map to
        SchemaNode schemaNode = model.getSyntaxModel().getNodes().stream().filter(node -> !(node instanceof PrimitiveNode)).findFirst().orElseThrow();

        // check that the node is a valid candidate
        assertFalse(schemaNode instanceof PrimitiveNode);
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getPrimitiveUUID().equals(schemaNode.getUuid())));
        assertTrue(model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .noneMatch(node -> Objects.equals(node.getSyntaxNodeUuid(), schemaNode.getUuid())));

        // create the new type and concept
        Class timeConcept = new Class("plasma:time", "time", "A time");
        Instance timeType = new Instance(timeConcept.getLabel(), "", timeConcept);
        timeType.setSyntaxNodeUuid(schemaNode.getUuid());

        DeltaModification modification = new DeltaModification("mod_testApplyMapOneEntityToNonPrimitiveNode", Collections.singleton(timeType), null, null, null, 0.0);

        assertThrows(UnsupportedOperationException.class, () -> model.apply(modification), "Error message not thrown when mapping to non-primitive node " + schemaNode.getUuid());

    }

    @Test
    public void testApplyUnmapOneEntityFromSyntaxNode() {
        CombinedModel model = getCombinedModel();

        Instance iataCodeType = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(instance -> instance.getSemanticClass().getURI().equals("plasma:iata_code")).findFirst().orElseThrow();

        Instance replacementType = iataCodeType.copy();

        replacementType.setSyntaxNodeUuid(null);
        // we leave the original label set here on purpose...

        DeltaModification modification = new DeltaModification("mod_testApplyUnmapOneEntityFromSyntaxNode", Collections.singleton(replacementType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        Instance replacementEntity = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(Instance -> Objects.equals(Instance.getUuid(), replacementType.getUuid())).findFirst().orElseThrow();
        assertEquals(replacementType.getUuid(), replacementEntity.getUuid(), "Replacement type does not have the same id as replaced entity");
        assertNull(replacementEntity.getOriginalLabel(), "New entity type's original label is not unset");
        assertNull(replacementEntity.getSyntaxNodeUuid(), "New entity type's mapping uuid is not unset");

        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // ensure that the explicit mapping from entity to syntax has been deleted
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getInstanceId().equals(replacementEntity.getUuid())));
    }

    @Test
    public void testApplyRemoveEntityAndRelation() {
        CombinedModel model = getCombinedModel();

        Instance airlineType = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(instance -> instance.getSemanticClass().getURI().equals("plasma:airline")).findFirst().orElseThrow();
        Instance nameType = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Instance)
                .map(node -> (Instance) node)
                .filter(instance -> instance.getSemanticClass().getURI().equals("plasma:name")).findFirst().orElseThrow();

        Relation airlineNameRelation = model.getSemanticModel().getEdges().stream().filter(relation -> relation.getFromId().equals(airlineType.getUuid()) && relation.getToId().equals(nameType.getUuid())).findFirst().orElseThrow();

        DeltaModification modification = new DeltaModification("mod_testApplyRemoveOneEntityAndOneRelation", Set.of(airlineType, nameType), Collections.singleton(airlineNameRelation), null, null, 0.0);
        modification.setDeletion(true);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() - 2, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertFalse(model.getSemanticModel().getNodes().contains(airlineType), "New entity type is still available");
        assertFalse(model.getSemanticModel().getEdges().contains(airlineNameRelation), "New relation is still available");
        // check that connecting edges are also deleted
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size() - 3, model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // ensure that the mapping from name to syntax has been deleted
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getInstanceId().equals(nameType.getUuid())));
    }


}