package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CombinedModelTest {

    @Test
    public void testApplyFailsIfFinalized() {
        CombinedModel model = getCombinedModel();
        model.finalizeModel();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        assertThrows(UnsupportedOperationException.class, () -> model.apply(modification));
    }

    @Test
    public void testApplyAddOneEntity() {
        CombinedModel model = getCombinedModel();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyType), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    @Test
    public void testApplyAddOneEntityAndOneRelation() {
        CombinedModel model = getCombinedModel();

        EntityType airlineType = model.getSemanticModel().getNodes().stream().filter(entityType -> entityType.getEntityConcept().getSourceURI().equals("plasma:airline")).findFirst().orElseThrow();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        RelationConcept isARelationConcept = new RelationConcept("isA", "Indicates that something is a superclass of something", "plasma:isA", new HashSet<>());
        Relation airlineCompanyRelation = new Relation(airlineType.getUuid(), companyType.getUuid(), isARelationConcept);

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

        EntityType iataCodeType = model.getSemanticModel().getNodes().stream().filter(entityType -> entityType.getEntityConcept().getSourceURI().equals("plasma:iata_code")).findFirst().orElseThrow();

        EntityConcept codeConcept = new EntityConcept("code", "A sort of identifier or short version of text", "plasma:code");
        EntityType codeType = new EntityType(iataCodeType.getUuid(),
                iataCodeType.getEntityId(),
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

        EntityType replacedEntity = model.getSemanticModel().getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), codeType.getUuid())).findFirst().orElseThrow();
        assertEquals(replacedEntity.getUuid(), iataCodeType.getUuid(),"Replacement type does not have the same id as replaced entity");
        assertEquals(codeConcept.getSourceURI(),replacedEntity.getEntityConcept().getSourceURI(), "New entity concept is not assigned");
        assertEquals(codeType.getDescription(),replacedEntity.getDescription(), "New entity description is not applied correctly");
        assertTrue(model.getSemanticModel().getNodes().stream().noneMatch(entityType -> entityType.getLabel().equals(iataCodeType.getLabel())), "Old entity type is not completely replaced");
        assertTrue(model.getSemanticModel().getNodes().stream().noneMatch(entityType -> entityType.getEntityConcept().getSourceURI().equals(iataCodeType.getEntityConcept().getSourceURI())), "Old entity concept is still lingering");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    private CombinedModel getCombinedModel() {
        SchemaNode root = new ObjectNode("ROOT", new ArrayList<>(), true);
        PrimitiveNode identifierNode = new PrimitiveNode("ident", null, true, DataType.STRING.name(), null, null);
        ObjectNode companyNode = new ObjectNode("carrier", null, true);
        PrimitiveNode nameNode = new PrimitiveNode("name", null, true, DataType.STRING.name(), null, null);

        SetNode codesNode = new SetNode(null, true);
        PrimitiveNode codeNode = new PrimitiveNode("codes1", null, true, DataType.STRING.name(), null, null);

        List<SchemaNode> nodes = Arrays.asList(root, identifierNode, companyNode, nameNode, codesNode, codeNode);

        Edge rootIdentifierEdge = new Edge(root.getUuid(), identifierNode.getUuid());
        Edge rootCompanyEdge = new Edge(root.getUuid(), companyNode.getUuid());
        Edge companyNameEdge = new Edge(companyNode.getUuid(), nameNode.getUuid());
        Edge companyCodesEdge = new Edge(companyNode.getUuid(), codeNode.getUuid());
        Edge codesCodeEdge = new Edge(codesNode.getUuid(), codeNode.getUuid());

        List<Edge> edges = Arrays.asList(rootIdentifierEdge, rootCompanyEdge, companyNameEdge, companyCodesEdge, codesCodeEdge);

        SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), nodes, edges);

        // generate semantic model

        EntityConcept flightConcept = new EntityConcept("flight", "A scheduled flight of an airplane", "plasma:flight");
        EntityType flightType = new EntityType(flightConcept.getLabel(), "", flightConcept);

        EntityConcept identifierConcept = new EntityConcept("identifier", "Identifies a certain object", "plasma:identifier");
        EntityType identifierType = new EntityType(identifierConcept.getLabel(), "", identifierConcept);
        identifierType.setOriginalLabel(identifierNode.getLabel());
        identifierType.setSyntaxNodeUuid(identifierNode.getUuid());

        EntityConcept airlineConcept = new EntityConcept("airline", "A company that provides air travel services", "plasma:airline");
        EntityType airlineType = new EntityType(airlineConcept.getLabel(), "", airlineConcept);

        EntityConcept nameConcept = new EntityConcept("name", "A commonly known but not unique identifier of something", "plasma:name");
        EntityType nameType = new EntityType(nameConcept.getLabel(), "", nameConcept);
        nameType.setOriginalLabel(nameNode.getLabel());
        nameType.setSyntaxNodeUuid(nameNode.getUuid());

        EntityConcept iataCodeConcept = new EntityConcept("IATA code", "A unique identifier issued by the IATA", "plasma:iata_code");
        EntityType iataCodeType = new EntityType(iataCodeConcept.getLabel(), "", iataCodeConcept);
        iataCodeType.setOriginalLabel(codeNode.getLabel());
        iataCodeType.setSyntaxNodeUuid(codeNode.getUuid());

        List<EntityType> entities = Arrays.asList(flightType, identifierType, airlineType, nameType, iataCodeType);

        RelationConcept hasRelationConcept = new RelationConcept("has", "Indicates that something is in possession of something or has something associated or assigned to it", "plasma:has", new HashSet<>());
        Relation flightIdentifierRelation = new Relation(flightType.getUuid(), identifierType.getUuid(), hasRelationConcept);
        Relation airlineNameRelation = new Relation(airlineType.getUuid(), nameType.getUuid(), hasRelationConcept);
        Relation airlineIATACodeRelation = new Relation(airlineType.getUuid(), iataCodeType.getUuid(), hasRelationConcept);

        RelationConcept operatesRelationConcept = new RelationConcept("operates", "Indicates that something is managed by something else", "plasma:operates", new HashSet<>());
        Relation flightAirlineRelation = new Relation(flightType.getUuid(), airlineType.getUuid(), operatesRelationConcept);

        List<Relation> relations = Arrays.asList(flightIdentifierRelation, flightAirlineRelation, airlineNameRelation, airlineIATACodeRelation);

        SemanticModel semanticModel = new SemanticModel(entities, relations);

        PrimitiveEntityTypeMapping identifierMapping = new PrimitiveEntityTypeMapping(identifierNode.getUuid(), identifierType.getUuid(), root.getUuid());
        PrimitiveEntityTypeMapping nameMapping = new PrimitiveEntityTypeMapping(nameNode.getUuid(), nameType.getUuid(), companyNode.getUuid());
        PrimitiveEntityTypeMapping codeMapping = new PrimitiveEntityTypeMapping(codeNode.getUuid(), iataCodeType.getUuid(), codesNode.getUuid());

        List<PrimitiveEntityTypeMapping> mappings = Arrays.asList(identifierMapping, nameMapping, codeMapping);

        CombinedModel combinedModel = new CombinedModel(syntaxModel, semanticModel, mappings, new ArrayList<>(), false);

        return combinedModel;
    }

}