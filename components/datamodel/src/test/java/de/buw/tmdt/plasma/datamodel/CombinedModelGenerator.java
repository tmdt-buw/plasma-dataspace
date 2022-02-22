package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CombinedModelGenerator {

    private CombinedModelGenerator() {

    }

    public static CombinedModel getCombinedModel() {
        SchemaNode root = new ObjectNode("ROOT", new ArrayList<>(), true);
        List<String> examples = Arrays.asList("10-11;suffix1", "20-21;suffix2", "test-test;suffix3");
        PrimitiveNode identifierNode = new PrimitiveNode("ident", null, true, DataType.String, examples, null);
        PrimitiveNode timeNode = new PrimitiveNode("time", null, true, DataType.String, null, null);
        ObjectNode companyNode = new ObjectNode("carrier", null, true);
        PrimitiveNode nameNode = new PrimitiveNode("name", null, true, DataType.String, null, null);

        SetNode codesNode = new SetNode(null, true);
        PrimitiveNode codeNode = new PrimitiveNode("codes1", null, true, DataType.String, null, null);

        List<SchemaNode> nodes = Arrays.asList(root, identifierNode, timeNode, companyNode, nameNode, codesNode, codeNode);

        Edge rootIdentifierEdge = new Edge(root.getUuid(), identifierNode.getUuid());
        Edge rootTimeEdge = new Edge(root.getUuid(), timeNode.getUuid());
        Edge rootCompanyEdge = new Edge(root.getUuid(), companyNode.getUuid());
        Edge companyNameEdge = new Edge(companyNode.getUuid(), nameNode.getUuid());
        Edge companyCodesEdge = new Edge(companyNode.getUuid(), codeNode.getUuid());
        Edge codesCodeEdge = new Edge(codesNode.getUuid(), codeNode.getUuid());

        List<Edge> edges = Arrays.asList(rootIdentifierEdge, rootTimeEdge, rootCompanyEdge, companyNameEdge, companyCodesEdge, codesCodeEdge);

        SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), nodes, edges);

        // generate semantic model

        SemanticClass flightClass = new SemanticClass("plasma:flight", "flight", "A scheduled flight of an airplane");
        Instance flightInstance = new Instance(flightClass.getLabel(), "", flightClass);

        Literal flightType = new Literal("domestic");

        SemanticClass identifierClass = new SemanticClass("plasma:identifier", "identifier", "Identifies a certain object");
        Instance identifierInstance = new Instance(identifierClass.getLabel(), "", identifierClass);
        identifierInstance.setOriginalLabel(identifierNode.getLabel());
        identifierInstance.setSyntaxNodeUuid(identifierNode.getUuid());

        SemanticClass airlineClass = new SemanticClass("plasma:airline", "airline", "A company that provides air travel services");
        Instance airlineInstance = new Instance(airlineClass.getLabel(), "", airlineClass);

        SemanticClass nameClass = new SemanticClass("plasma:name", "name", "A commonly known but not unique identifier of something");
        Instance nameInstance = new Instance(nameClass.getLabel(), "", nameClass);
        nameInstance.setOriginalLabel(nameNode.getLabel());
        nameInstance.setSyntaxNodeUuid(nameNode.getUuid());

        SemanticClass iataCodeClass = new SemanticClass("plasma:iata_code", "IATA code", "A unique identifier issued by the IATA");
        Instance iataCodeInstance = new Instance(iataCodeClass.getLabel(), "", iataCodeClass);
        iataCodeInstance.setOriginalLabel(codeNode.getLabel());
        iataCodeInstance.setSyntaxNodeUuid(codeNode.getUuid());

        NamedEntity iataClass = new NamedEntity("plasma:IATA", "IATA", "The International Air Transport Association");

        List<SemanticModelNode> elements = Arrays.asList(flightInstance, identifierInstance, airlineInstance, nameInstance, iataCodeInstance, flightType, iataClass);

        Relation flightIdentifierRelation = new ObjectProperty(flightInstance.getUuid(), identifierInstance.getUuid(), "plasma:has");
        Relation airlineNameRelation = new ObjectProperty(airlineInstance.getUuid(), nameInstance.getUuid(), "plasma:has");
        Relation airlineIATACodeRelation = new ObjectProperty(airlineInstance.getUuid(), iataCodeInstance.getUuid(), "plasma:has");
        Relation iataCodeIATARelation = new ObjectProperty(iataCodeInstance.getUuid(), iataClass.getUuid(), "plasma:issuedBy");
        Relation flightTypeRelation = new DataProperty(flightInstance.getUuid(), flightType.getUuid(), "plasma:flightType");

        Relation flightAirlineRelation = new ObjectProperty(flightInstance.getUuid(), airlineInstance.getUuid(), "plasma:operates");

        List<Relation> objectProperties = Arrays.asList(flightIdentifierRelation, flightAirlineRelation, airlineNameRelation, airlineIATACodeRelation, iataCodeIATARelation, flightTypeRelation);

        SemanticModel semanticModel = new SemanticModel(null, elements, objectProperties);


        CombinedModel combinedModel = new CombinedModel(UUID.randomUUID().toString(), syntaxModel, semanticModel, new ArrayList<>(), false);

        return combinedModel;
    }
}

