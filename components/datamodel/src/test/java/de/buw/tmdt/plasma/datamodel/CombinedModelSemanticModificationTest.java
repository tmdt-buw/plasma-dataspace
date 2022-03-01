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

        Class companyClass = new Class("plasma:company", "company", "A company");

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneClass", Collections.singleton(companyClass), null, null, null, 0.0);

        assertThrows(UnsupportedOperationException.class, () -> model.apply(modification));
    }

    @Test
    public void testApplyAddOneEntity() {
        CombinedModel model = getCombinedModel();

        Class companyClass = new Class("plasma:company", "company", "A company");

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneClass", Collections.singleton(companyClass), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyClass), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    @Test
    public void testApplyAddOneEntityAndOneRelation() {
        CombinedModel model = getCombinedModel();

        Class airlineClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(clazz -> clazz.getURI().equals("plasma:airline"))
                .findFirst()
                .orElseThrow();

        Class companyClass = new Class("plasma:company", "company", "A company");

        Relation airlineCompanyRelation = new ObjectProperty(airlineClass.getUuid(), companyClass.getUuid(), "plasma:isA");

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntityAndOneRelation", Collections.singleton(companyClass), Collections.singleton(airlineCompanyRelation), null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyClass), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size() + 1, model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
        assertTrue(model.getSemanticModel().getEdges().contains(airlineCompanyRelation), "New relation is not available");
    }

    @Test
    public void testApplyReplaceOneNode() {
        CombinedModel model = getCombinedModel();

        Class iataCodeClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(clazz -> clazz.getURI().equals("plasma:iata_code"))
                .findFirst()
                .orElseThrow();

        Instance iataCodeInstance = iataCodeClass.getInstance();

        assertNotNull(iataCodeInstance);

        Class codeClass = new Class(iataCodeClass.getUuid(),
                "plasma:code",
                "code",
                "A sort of identifier or short version of text",
                iataCodeClass.getXCoordinate(),
                iataCodeClass.getYCoordinate()
        );
        Instance codeInstance = new Instance(
                null,
                null,
                "code",
                iataCodeInstance.getSyntaxNodeUuid(),
                iataCodeInstance.getOriginalLabel(),
                "The code of this airline"
        );
        codeClass.setInstance(codeInstance);

        DeltaModification modification = new DeltaModification("mod_testApplyReplaceOneEntity", Collections.singleton(codeClass), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        Class replacedNode = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(clazz -> Objects.equals(clazz.getUuid(), codeClass.getUuid()))
                .findFirst()
                .orElseThrow();

        assertEquals(replacedNode.getUuid(), iataCodeClass.getUuid(), "Replacement entity does not have the same id as replaced entity");
        assertEquals(codeClass.getURI(), replacedNode.getURI(), "New class is not assigned");
        assertEquals(codeClass.getDescription(), replacedNode.getDescription(), "New description is not applied correctly");
        assertNotNull(replacedNode.getInstance());
        Instance replacedInstance = replacedNode.getInstance();
        assertEquals(replacedInstance.getDescription(), codeInstance.getDescription(), "New instance description is not applied correctly");
        assertEquals(replacedInstance.getLabel(), codeInstance.getLabel(), "New instance label is not applied correctly");

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
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .map(Class::getInstance)
                .filter(Objects::nonNull)
                .noneMatch(node -> Objects.equals(node.getSyntaxNodeUuid(), timeNode.getUuid())));

        // create the new class to assign
        Class timeClass = new Class("plasma:time", "time", "A time");
        Instance timeInstance = new Instance(timeNode.getLabel(), "");
        timeInstance.setSyntaxNodeUuid(timeNode.getUuid());
        timeInstance.setOriginalLabel(timeNode.getLabel());
        timeClass.setInstance(timeInstance);

        DeltaModification modification = new DeltaModification("mod_testApplyMapOneClassToPrimitiveNode", Collections.singleton(timeClass), null, null, null, 0.0);

        model.apply(modification);

        // check numbers
        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // check properties
        Instance addedTimeInstance = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .map(Class::getInstance)
                .filter(Objects::nonNull)
                .filter(instance -> Objects.equals(instance.getUuid(), timeInstance.getUuid())).findFirst().orElseThrow();
        assertEquals(timeInstance.getUuid(), addedTimeInstance.getUuid(), "Added class does not have the same id as added class");
        assertEquals(timeNode.getLabel(), addedTimeInstance.getOriginalLabel(), "New instance's original label is not set properly");
        assertEquals(timeNode.getUuid(), addedTimeInstance.getSyntaxNodeUuid(), "New instance's mapping uuid is not set properly");

        // ensure that the explicit mapping from class and instance to syntax has been created
        assertTrue(model.generatePrimitiveInstanceMappings().stream().anyMatch(mapping -> mapping.getInstanceId().equals(addedTimeInstance.getUuid())));
    }

    @Test
    public void testApplyMapOneEntityToNonPrimitiveNode() {
        CombinedModel model = getCombinedModel();

        // get the target node we want to map to
        SchemaNode schemaNode = model.getSyntaxModel().getNodes().stream()
                .filter(node -> !(node instanceof PrimitiveNode))
                .findFirst()
                .orElseThrow();

        // check that the node is a valid candidate
        assertFalse(schemaNode instanceof PrimitiveNode);
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getPrimitiveUUID().equals(schemaNode.getUuid())));
        assertTrue(model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .map(Class::getInstance)
                .filter(Objects::nonNull)
                .noneMatch(instance -> Objects.equals(instance.getSyntaxNodeUuid(), schemaNode.getUuid())));

        // create the new type and concept
        Class timeClass = new Class("plasma:time", "time", "A time");
        Instance timeInstance = new Instance(timeClass.getLabel(), "");
        timeClass.setInstance(timeInstance);
        timeInstance.setSyntaxNodeUuid(schemaNode.getUuid());

        DeltaModification modification = new DeltaModification("mod_testApplyMapOneEntityToNonPrimitiveNode", Collections.singleton(timeClass), null, null, null, 0.0);

        assertThrows(CombinedModelIntegrityException.class, () -> model.apply(modification), "Error message not thrown when mapping to non-primitive node " + schemaNode.getUuid());
    }

    @Test
    public void testApplyUnmapOneEntityFromSyntaxNode() {
        CombinedModel model = getCombinedModel();

        Class iataCodeClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(instance -> instance.getURI().equals("plasma:iata_code")).findFirst().orElseThrow();

        Class replacementClass = iataCodeClass.copy();
        assertNotNull(replacementClass.getInstance());
        replacementClass.getInstance().setSyntaxNodeUuid(null);
        replacementClass.getInstance().setOriginalLabel(null);

        DeltaModification modification = new DeltaModification("mod_testApplyUnmapOneEntityFromSyntaxNode", Collections.singleton(replacementClass), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        Class retrievedReplacementClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(clazz -> Objects.equals(clazz.getUuid(), replacementClass.getUuid()))
                .findFirst()
                .orElseThrow();
        assertEquals(retrievedReplacementClass.getUuid(), retrievedReplacementClass.getUuid(), "Replacement entity does not have the same id as replaced entity");

        Instance i = retrievedReplacementClass.getInstance();
        assertNotNull(i);
        assertNull(i.getOriginalLabel(), "New instance's original label is not unset");
        assertNull(i.getSyntaxNodeUuid(), "New instance's mapping uuid is not unset");

        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // ensure that the explicit mapping from entity to syntax node does not get generated
        assertTrue(model.generatePrimitiveInstanceMappings().stream()
                .noneMatch(mapping -> mapping.getInstanceId().equals(i.getUuid())));
    }

    @Test
    public void testApplyRemoveEntityAndRelation() {
        CombinedModel model = getCombinedModel();

        Class airlineClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(clazz -> clazz.getURI().equals("plasma:airline"))
                .findFirst()
                .orElseThrow();

        Class nameClass = model.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .filter(instance -> instance.getURI().equals("plasma:name"))
                .findFirst()
                .orElseThrow();

        Relation airlineNameRelation = model.getSemanticModel().getEdges().stream()
                .filter(relation -> relation.getFromId().equals(airlineClass.getUuid())
                        && relation.getToId().equals(nameClass.getUuid()))
                .findFirst()
                .orElseThrow();

        DeltaModification modification = new DeltaModification("mod_testApplyRemoveOneEntityAndOneRelation",
                Set.of(airlineClass, nameClass),
                Collections.singleton(airlineNameRelation),
                null,
                null,
                0.0);
        modification.setDeletion(true);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() - 2, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertFalse(model.getSemanticModel().getNodes().contains(airlineClass), "New entity type is still available");
        assertFalse(model.getSemanticModel().getEdges().contains(airlineNameRelation), "New relation is still available");
        // check that connecting edges are also deleted
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size() - 3, model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");

        // ensure that the mapping from name to syntax has been deleted
        assertTrue(model.generatePrimitiveInstanceMappings().stream().noneMatch(mapping -> mapping.getInstanceId().equals(nameClass.getUuid())));
    }


}