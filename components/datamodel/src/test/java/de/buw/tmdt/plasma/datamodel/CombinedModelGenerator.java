package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CombinedModelGenerator {

    public static CombinedModel getCombinedModel() {
        SchemaNode root = new ObjectNode("ROOT", new ArrayList<>(), true);
        List<String> examples = Arrays.asList("10-11", "20-21", "test-test");
        PrimitiveNode identifierNode = new PrimitiveNode("ident", null, true, DataType.STRING.name(), examples, null);
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
