package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An instance of this class uses a given {@link CombinedModel} and injects data from a data source into the values
 * (literals) of the contained semantic model.
 * Injection is performed by replacing or adding literals to contain the data.
 */
public class DataInjector {

    private static final Logger log = LoggerFactory.getLogger(DataInjector.class);

    private static final int NODE_LABEL_LENGTH = 254;
    private static final String ROOT_ELEMENT_NAME = "ROOT";

    private Map<PrimitiveNode, String> valueMapTemplate;
    private Map<PrimitiveNode, String> primitiveValueMap;


    public List<SemanticModel> inject(CombinedModel combinedModel, List<JsonNode> data) throws ConversionException {
        List<SemanticModel> results = new ArrayList<>(data.size());
        for (JsonNode datum : data) {
            results.add(inject(combinedModel, datum));
        }
        return results;
    }

    public SemanticModel inject(CombinedModel combinedModel,JsonNode datum) throws ConversionException {
        if( valueMapTemplate == null || valueMapTemplate.isEmpty()) {
            this.valueMapTemplate = buildValueMapTemplate(combinedModel);
        }
        buildValueMap(datum);

        try {
            // map syntax UUID -> value
            Map<String, String> uuidValueMap = primitiveValueMap.entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getUuid(), Map.Entry::getValue));
            DeltaModification delta = new DeltaModification("mappingConversion");
            // now remove all unneeded elements from the semantic model and replace mapped nodes by values
            for (SemanticModelNode node : combinedModel.getSemanticModel().getNodes()) {
                if(node instanceof Class){
                    Class clazz = (Class) node;
                    if(!clazz.isMapped()){
                        continue;
                    }
                    String syntaxNodeUuid = clazz.getMappedSyntaxNodeUuid();
                    if(!uuidValueMap.containsKey(syntaxNodeUuid)){
                        throw new ConversionException("Could not find value for syntaxId " + syntaxNodeUuid + " on model " + combinedModel.getId());
                    }
                    Literal literal = new Literal(uuidValueMap.get(syntaxNodeUuid));
                    DataProperty prop =new DataProperty(clazz.getUuid(), literal.getUuid(), "http://plasma.uni-wuppertal.de/ontology#hasValue");
                    delta.getEntities().add(literal);
                    delta.getRelations().add(prop);
                    continue;
                }
                if(node instanceof Literal){
                    Literal lit = (Literal) node;
                    if(!lit.isMapped()){
                        continue;
                    }
                    String syntaxNodeUuid = lit.getMappedSyntaxNodeUuid();
                    if(!uuidValueMap.containsKey(syntaxNodeUuid)){
                        throw new ConversionException("Could not find value for syntaxId " + syntaxNodeUuid + " on model " + combinedModel.getId());
                    }
                    lit.setLabel(uuidValueMap.get(syntaxNodeUuid));
                    delta.getEntities().add(lit);
                }
            }

            combinedModel.apply(delta);

        } catch (ConversionException te){
            log.warn("Error during parsing of datum", te);
        }

        return combinedModel.getSemanticModel();
    }

    private Map<PrimitiveNode, String> buildValueMapTemplate(CombinedModel combinedModel) {
        Map<PrimitiveNode, String> valueMapTemplate = new HashMap<>();
        // initialize template with all mapped primitive nodes
        combinedModel.getSemanticModel().getNodes().stream()
                .filter(SemanticModelNode::isMapped)
                .map(node -> (MappableSemanticModelNode) node)
                .map(MappableSemanticModelNode::getMappedSyntaxNodeUuid)
                .map(id -> combinedModel.getSyntaxModel().getNode(id))
                .filter(Objects::nonNull)
                .map(node -> (PrimitiveNode) node)
                .forEach(primitive -> valueMapTemplate.put(primitive, null));
        return valueMapTemplate;
    }


    private void buildValueMap(JsonNode data  ) throws ConversionException {
        List<String> path = new ArrayList<>();
        primitiveValueMap = new HashMap<>(valueMapTemplate);
        traverseJsonElement(data, ROOT_ELEMENT_NAME, path);
        // validate that there are no unmapped values
        long notMappedNodesCount = primitiveValueMap.values().stream()
                .filter(Objects::isNull)
                .count();
        if(notMappedNodesCount > 0){
            throw new ConversionException("There are " + notMappedNodesCount + " unmapped primitives in this datum");
        }
    }

    private void traverseJsonElement(JsonNode jsonNode, String label, List<String> path) throws ConversionException {
        if (jsonNode.isNull()) {
            return;
        } else if (jsonNode.isValueNode()) {
            readJSONPrimitive(jsonNode, label, path);
            return;
        } else if (jsonNode.isObject()) {
            traverseJsonObject((ObjectNode) jsonNode, label, path);
            return;
        } else if (jsonNode.isArray()) {
            traverseJsonArray((ArrayNode) jsonNode, label, path);
            return;
        }
        throw new ConversionException("Unknown JSON SubType: \"" + jsonNode + "\".");
    }

    private void readJSONPrimitive(JsonNode jsonNode, String label, List<String> path) {
        Optional<PrimitiveNode> match = primitiveValueMap.keySet().stream()
                .filter(primitive -> Objects.equals(primitive.getLabel(), label)
                 //       &&                        Objects.equals(primitive.getPath(), path)
                )
                .findFirst();
        if (match.isPresent()) {
            // this is a mapped node
            String value = jsonNode.asText();
            primitiveValueMap.put(match.get(), value);
        } else {
            log.warn("ValueNode '{}' {} is not mapped", label, path);
        }
    }


    private void traverseJsonObject(ObjectNode object, String label, List<String> path) throws ConversionException {
        path = new ArrayList<>(path);
        path.add(label);

        for (Iterator<Map.Entry<String, JsonNode>> it = object.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String nodeLabel = entry.getKey().substring(0, Math.min(entry.getKey().length(), NODE_LABEL_LENGTH));
            traverseJsonElement(entry.getValue(), nodeLabel, path);
        }
    }

    private void traverseJsonArray(ArrayNode array, String label, List<String> path) throws ConversionException {
        path = new ArrayList<>(path);
        path.add(label);
        for (JsonNode node : array) {
            traverseJsonElement(node, "[]", path);
        }
    }
}
