package de.buw.tmdt.plasma.datamodel.generators;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Generates a basic {@link CombinedModel}.
 * Can be used for testing purposes.
 */
public class CombinedModelGenerator {

    public static final String NAMESPACE = "http://plasma.uni-wuppertal.de/ontology#";

    private CombinedModelGenerator() {

    }

    public static String generateResourceURI(String label) {
        return NAMESPACE + label;
    }

    public static CombinedModel getCombinedModel() {
        return getCombinedModel(NAMESPACE);
    }

    public static CombinedModel getCombinedModel(String namespace) {
        SyntaxNode root = new ObjectNode("ROOT");
        List<String> examples = Arrays.asList("10-11;suffix1", "20-21;suffix2", "test-test;suffix3");
        PrimitiveNode identifierNode = new PrimitiveNode("ident",  DataType.String, examples, null);
        PrimitiveNode timeNode = new PrimitiveNode("time",  DataType.String, null, null);
        ObjectNode companyNode = new ObjectNode("carrier");
        PrimitiveNode nameNode = new PrimitiveNode("name",  DataType.String, null, null);

        SetNode codesNode = new SetNode();
        PrimitiveNode codeNode = new PrimitiveNode("codes1",  DataType.String, null, null);

        List<SyntaxNode> nodes = Arrays.asList(root, identifierNode, timeNode, companyNode, nameNode, codesNode, codeNode);

        Edge rootIdentifierEdge = new Edge(root.getUuid(), identifierNode.getUuid());
        Edge rootTimeEdge = new Edge(root.getUuid(), timeNode.getUuid());
        Edge rootCompanyEdge = new Edge(root.getUuid(), companyNode.getUuid());
        Edge companyNameEdge = new Edge(companyNode.getUuid(), nameNode.getUuid());
        Edge companyCodesEdge = new Edge(companyNode.getUuid(), codeNode.getUuid());
        Edge codesCodeEdge = new Edge(codesNode.getUuid(), codeNode.getUuid());

        List<Edge> edges = Arrays.asList(rootIdentifierEdge, rootTimeEdge, rootCompanyEdge, companyNameEdge, companyCodesEdge, codesCodeEdge);

        SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), nodes, edges);

        // generate semantic model

        Class flightClass = new Class(namespace + "flight", "flight", "A scheduled flight of an airplane");

        Literal flightTypeLiteral = new Literal("domestic");
        flightTypeLiteral.setXCoordinate(100.0);
        flightTypeLiteral.setYCoordinate(100.0);

        Literal identifierLiteral = new Literal(namespace + "identifier");
        identifierLiteral.setMappedSyntaxNodeLabel(identifierNode.getLabel());
        identifierLiteral.setMappedSyntaxNodeUuid(identifierNode.getUuid());

        Class airlineClass = new Class(namespace + "airline", "airline", "A company that provides air travel services");

        Literal nameLiteral = new Literal(namespace + "name");
        nameLiteral.setMappedSyntaxNodeLabel(nameNode.getLabel());
        nameLiteral.setMappedSyntaxNodeUuid(nameNode.getUuid());

        Literal iataCodeLiteral = new Literal(namespace + "iata_code");
        iataCodeLiteral.setMappedSyntaxNodeLabel(codeNode.getLabel());
        iataCodeLiteral.setMappedSyntaxNodeUuid(codeNode.getUuid());

        NamedEntity iataClass = new NamedEntity(namespace + "IATA", "IATA", "The International Air Transport Association");

        List<SemanticNode> elements = Arrays.asList(identifierLiteral, flightClass, flightTypeLiteral, airlineClass, nameLiteral, iataCodeLiteral, iataClass);

        Relation flightIdentifierRelation = new ObjectProperty(flightClass.getUuid(), identifierLiteral.getUuid(), namespace + "has");
        Relation airlineNameRelation = new ObjectProperty(airlineClass.getUuid(), nameLiteral.getUuid(), namespace + "has");
        Relation airlineIATACodeRelation = new ObjectProperty(airlineClass.getUuid(), iataCodeLiteral.getUuid(), namespace + "has");
        Relation iataCodeIATARelation = new ObjectProperty(iataCodeLiteral.getUuid(), iataClass.getUuid(), namespace + "issuedBy");
        Relation flightTypeRelation = new DataProperty(flightClass.getUuid(), flightTypeLiteral.getUuid(), namespace + "flightType");
        Relation flightAirlineRelation = new ObjectProperty(airlineClass.getUuid(), flightClass.getUuid(), namespace + "operates");

        List<Relation> objectProperties = Arrays.asList(flightIdentifierRelation, flightAirlineRelation, airlineNameRelation, airlineIATACodeRelation, iataCodeIATARelation, flightTypeRelation);

        SemanticModel semanticModel = new SemanticModel(null, elements, objectProperties);
        CombinedModel combinedModel = new CombinedModel(UUID.randomUUID().toString(), syntaxModel, semanticModel, new ArrayList<>(), false);

        combinedModel.validate();

        return combinedModel;
    }
}

