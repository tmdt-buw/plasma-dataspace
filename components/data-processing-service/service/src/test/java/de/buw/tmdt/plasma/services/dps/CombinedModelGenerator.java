package de.buw.tmdt.plasma.services.dps;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import org.apache.jena.rdf.model.Model;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombinedModelGenerator {

    public static final String namespace = "http://local.host/ontology#";
    public static final String ROOT_INDICATOR = "";
    private static final String ARRAY_ELEMENT_NAME = "0";

    private boolean mapIdentificationNumbers = false;
    private boolean mapIdentificationNumberInstance = false;
    private boolean mapStaffInstance = false;
    private boolean mapVesselInformation = true;


    public CombinedModel getFlightModel() {

        List<SchemaNode> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        SchemaNode root = new ObjectNode("root",List.of(ROOT_INDICATOR), null, null, true);
        nodes.add(root);
        PrimitiveNode identifierNode = new PrimitiveNode("id", List.of(ROOT_INDICATOR, "id"),true, DataType.String, List.of("AL0025-20201123-1456-1634"), null);
        Edge rootIdentifierEdge = new Edge(root.getUuid(), identifierNode.getUuid());
        nodes.add(identifierNode);
        edges.add(rootIdentifierEdge);

        ObjectNode vesselNode = new ObjectNode("vessel", true);
        vesselNode.setPath(List.of(ROOT_INDICATOR, vesselNode.getLabel()));
        PrimitiveNode vesselNameNode = new PrimitiveNode("vessel_name", true, DataType.String, List.of("Airbus A350 XPB"), null);
        vesselNameNode.setPath(List.of(ROOT_INDICATOR,vesselNode.getLabel(), vesselNameNode.getLabel()));
        ObjectNode ownerNode = new ObjectNode("owner", true);
        ownerNode.setPath(List.of(ROOT_INDICATOR,vesselNode.getLabel(),ownerNode.getLabel()));
        PrimitiveNode carrierNode = new PrimitiveNode("carrier", true, DataType.String, List.of("Lufthansa"), null);
        carrierNode.setPath(List.of(ROOT_INDICATOR,vesselNode.getLabel(),ownerNode.getLabel(), carrierNode.getLabel()));
        PrimitiveNode carrierCodeNode = new PrimitiveNode("carrier_code", true, DataType.String, List.of("LH"), null);
        carrierCodeNode.setPath(List.of(ROOT_INDICATOR,vesselNode.getLabel(),ownerNode.getLabel(), carrierCodeNode.getLabel()));
        Edge rootVesselEdge = new Edge(root.getUuid(), vesselNode.getUuid());
        Edge vesselNameEdge = new Edge(vesselNode.getUuid(), vesselNameNode.getUuid());
        Edge vesselOwnerEdge = new Edge(vesselNode.getUuid(), ownerNode.getUuid());
        Edge vesselOwnerCarrierEdge = new Edge(ownerNode.getUuid(), carrierNode.getUuid());
        Edge vesselOwnerCarrierCodeEdge = new Edge(ownerNode.getUuid(), carrierCodeNode.getUuid());
        nodes.addAll(List.of(vesselNode,vesselNameNode,ownerNode,carrierNode,carrierCodeNode));
        edges.addAll(List.of(rootVesselEdge,vesselNameEdge,vesselOwnerEdge,vesselOwnerCarrierEdge,vesselOwnerCarrierCodeEdge));


        ObjectNode originNode = new ObjectNode("origin",List.of(ROOT_INDICATOR), null, null, true);
        originNode.setPath(List.of(ROOT_INDICATOR, originNode.getLabel()));
        PrimitiveNode nameNode = new PrimitiveNode("airport_name", true, DataType.String, null, null);
        nameNode.setPath(List.of(ROOT_INDICATOR, originNode.getLabel(), nameNode.getLabel()));
        SetNode identificationNumbersNode = new SetNode(UUID.randomUUID().toString(), "identification_numbers", List.of(ROOT_INDICATOR), null, null, true, true, false);
        identificationNumbersNode.setPath(List.of(ROOT_INDICATOR, originNode.getLabel(), identificationNumbersNode.getLabel()));

        PrimitiveNode identificationNumberNode = new PrimitiveNode("number0", true, DataType.Number, null, null);
        identificationNumberNode.setPath(List.of(ROOT_INDICATOR, originNode.getLabel(), identificationNumbersNode.getLabel(), ARRAY_ELEMENT_NAME));
        Edge rootOriginEdge = new Edge(root.getUuid(), originNode.getUuid());
        Edge originNameEdge = new Edge(originNode.getUuid(), nameNode.getUuid());
        Edge originNumberEdge = new Edge(originNode.getUuid(), identificationNumberNode.getUuid());
        Edge numbersNumberEdge = new Edge(identificationNumbersNode.getUuid(), identificationNumberNode.getUuid());
        nodes.addAll(List.of(originNode,nameNode,identificationNumbersNode,identificationNumberNode));
        edges.addAll(List.of(rootOriginEdge,originNameEdge,originNumberEdge,numbersNumberEdge));

        SetNode staffNode = new SetNode(UUID.randomUUID().toString(), "staff", List.of(ROOT_INDICATOR), null, null, true, true, false);
        staffNode.setPath(List.of(ROOT_INDICATOR, staffNode.getLabel()));
        ObjectNode staffObjectNode = new ObjectNode(UUID.randomUUID().toString(), "object1", List.of(ROOT_INDICATOR, staffNode.getLabel(), ARRAY_ELEMENT_NAME), null, null, true, true, false);
        PrimitiveNode staffNameNode = new PrimitiveNode("name", true, DataType.Number, List.of(ROOT_INDICATOR, staffNode.getLabel(), ARRAY_ELEMENT_NAME), null);
        staffNameNode.setPath(List.of(ROOT_INDICATOR, staffNode.getLabel(), ARRAY_ELEMENT_NAME, staffNameNode.getLabel()));
        PrimitiveNode staffSeatNode = new PrimitiveNode("seat", true, DataType.Number, List.of(ROOT_INDICATOR, staffNode.getLabel(), ARRAY_ELEMENT_NAME), null);
        staffSeatNode.setPath(List.of(ROOT_INDICATOR, staffNode.getLabel(), ARRAY_ELEMENT_NAME, staffSeatNode.getLabel()));
        Edge rootStaffEdge = new Edge(root.getUuid(), staffNode.getUuid());
        Edge staffObjectEdge = new Edge(staffNode.getUuid(), staffObjectNode.getUuid());
        Edge staffNameEdge = new Edge(staffObjectNode.getUuid(),staffNameNode.getUuid());
        Edge staffSeatEdge = new Edge(staffObjectNode.getUuid(), staffSeatNode.getUuid());

        nodes.addAll(List.of(staffNode,staffObjectNode,staffNameNode,staffSeatNode));
        edges.addAll(List.of(rootStaffEdge,staffObjectEdge,staffNameEdge,staffSeatEdge));

        SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), nodes, edges);

        // generate semantic model
        List<SemanticModelNode> elements = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();
        Class flightClass = new Class(namespace + "flight", "flight", "A scheduled flight of an airplane");
        elements.add(flightClass);

        Literal flightTypeLiteral = new Literal("domestic");
        DataProperty flightTypeProperty = new DataProperty(flightClass.getUuid(), flightTypeLiteral.getUuid(),namespace + "flightType");
        elements.add(flightTypeLiteral);
        relations.add(flightTypeProperty);

        Class identifierClass = new Class(namespace + "identifier", "identifier", "Identifies a certain object");
        mapSemanticNode(identifierClass,identifierNode);
        Relation flightIdentifierRelation = new ObjectProperty(flightClass.getUuid(), identifierClass.getUuid(), namespace + "has");
        elements.add(identifierClass);
        relations.add(flightIdentifierRelation);

        if(this.mapVesselInformation){
            Class vesselClass = new Class(namespace + "airplane", "airplane", "A flying vessel");
            Literal nameLiteral = new Literal(vesselNameNode.getLabel());
            mapSemanticNode(nameLiteral, vesselNameNode);
            ObjectProperty flightVesselProperty = new ObjectProperty(flightClass.getUuid(), vesselClass.getUuid(), namespace + "performedBy");
            DataProperty vesselNameProperty = new DataProperty(vesselClass.getUuid(), nameLiteral.getUuid(), namespace + "hasName");
            Class airlineClass = new Class(namespace + "airline", "airline", "A company that provides air travel services");
            ObjectProperty vesselAirlineProperty = new ObjectProperty(vesselClass.getUuid(),airlineClass.getUuid(), namespace + "ownedBy");
            Literal airlineNameLiteral = new Literal(carrierNode.getLabel());
            mapSemanticNode(airlineNameLiteral, carrierNode);
            Relation airlineNameRelation = new DataProperty(airlineClass.getUuid(), airlineNameLiteral.getUuid(), namespace + "hasName");
            elements.addAll(List.of(vesselClass, nameLiteral,airlineClass, airlineNameLiteral));
            relations.addAll(List.of(flightVesselProperty,vesselNameProperty,vesselAirlineProperty,airlineNameRelation));

            if(this.mapIdentificationNumbers){
                Class identification_numbersClass = new Class(namespace + "ident_numbers", "identification numbers", "Some numbers");
                mapSemanticNode(identification_numbersClass, identificationNumbersNode);
                elements.add(identification_numbersClass);
                ObjectProperty airportNumbersProperty = new ObjectProperty(airlineClass.getUuid(),identification_numbersClass.getUuid(), namespace + "identNumbers");
                relations.add(airportNumbersProperty);
            } else if (this.mapIdentificationNumberInstance){
                Class identification_numbersInstanceClass = new Class(namespace + "ident_number", "identification number", "A single number");
                mapSemanticNode(identification_numbersInstanceClass, identificationNumberNode);
                elements.add(identification_numbersInstanceClass);
                ObjectProperty airportNumberInstanceProperty = new ObjectProperty(airlineClass.getUuid(),identification_numbersInstanceClass.getUuid(), namespace + "identNum");
                relations.add(airportNumberInstanceProperty);
            }
        }

        if(this.mapStaffInstance){
            Class staffClass = new Class(namespace + "staff", "staff","Personel that fulfills a task in a company.");
            mapSemanticNode(staffClass,staffObjectNode);
            Literal staffNameLiteral = new Literal("name");
            mapSemanticNode(staffNameLiteral, staffNameNode);
            Relation staffNameRelation = new DataProperty(staffClass.getUuid(), staffNameLiteral.getUuid(),namespace + "hasName");
            staffNameRelation.setIterative(true);
            Class seatClass = new Class(namespace + "seat_number", "seat", "Identifier for a specific place on a vehicle or in a room");
            mapSemanticNode(seatClass,staffSeatNode);
            Relation staffSeatRelation = new ObjectProperty(staffClass.getUuid(), seatClass.getUuid(),namespace + "seatedAt");
            staffSeatRelation.setIterative(true);
            // connect this bloc to the flight
            Relation flightStaffRelation = new ObjectProperty(flightClass.getUuid(), staffClass.getUuid(), namespace + "hasStaff");
            Relation staffFlightRelation = new ObjectProperty( staffClass.getUuid(),flightClass.getUuid(), namespace + "scheduledFor");
            elements.addAll(List.of(staffClass, staffNameLiteral, seatClass));
            relations.addAll(List.of(flightStaffRelation,staffFlightRelation,staffNameRelation,staffSeatRelation));
        }

        SemanticModel semanticModel = new SemanticModel(UUID.randomUUID().toString(), elements, relations);
        CombinedModel combinedModel = new CombinedModel(UUID.randomUUID().toString(), syntaxModel, semanticModel, new ArrayList<>(), false);

        combinedModel.validate();
        return combinedModel;
    }

    private static void mapSemanticNode(MappableSemanticModelNode semanticNode, MappableSyntaxNode schemaNode){
        semanticNode.setMappedSyntaxNodeLabel(schemaNode.getLabel());
        semanticNode.setMappedSyntaxNodeUuid(schemaNode.getUuid());
        semanticNode.setMappedSyntaxNodePath(schemaNode.getPathAsJSONPointer());
        if(semanticNode instanceof Class){
            Class clazz = (Class) semanticNode;
            if(clazz.getInstance() != null){
                Instance instance = new Instance(schemaNode.getLabel(), null);
                clazz.setInstance(instance);
            }
        }
    }

    public static String asTurtle(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TURTLE");
        return out.toString();
    }

    public boolean isMapIdentificationNumbers() {
        return mapIdentificationNumbers;
    }

    public void setMapIdentificationNumbers(boolean mapIdentificationNumbers) {
        this.mapIdentificationNumbers = mapIdentificationNumbers;
    }

    public boolean isMapIdentificationNumberInstance() {
        return mapIdentificationNumberInstance;
    }

    public void setMapIdentificationNumberInstance(boolean mapIdentificationNumberInstance) {
        this.mapIdentificationNumberInstance = mapIdentificationNumberInstance;
    }

    public boolean isMapStaffInstance() {
        return mapStaffInstance;
    }

    public void setMapStaffInstance(boolean mapStaffInstance) {
        this.mapStaffInstance = mapStaffInstance;
    }

    public boolean isMapVesselInformation() {
        return mapVesselInformation;
    }

    public void setMapVesselInformation(boolean mapVesselInformation) {
        this.mapVesselInformation = mapVesselInformation;
    }
}
