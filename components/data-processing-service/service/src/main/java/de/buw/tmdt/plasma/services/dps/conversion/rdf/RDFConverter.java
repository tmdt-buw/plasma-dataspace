package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Literal;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.MappableSyntaxNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converter to parse a {@link SemanticModel} with filled {@link Literal}s and convert it to RDF.
 */
public class RDFConverter {

    public static final Logger log = LoggerFactory.getLogger(RDFConverter.class);

    private static final int NODE_LABEL_LENGTH = 254;
    private static final String ROOT_ELEMENT_NAME = "$";

    private static final String MISSING_DATA_LITERAL = "##ValueNotAvailable##";
    private static final String ARRAY_LITERAL = "##NonPrimitiveArray##";

    private final PrefixMapping prefixes;
    int counter = 0;

    private Map<MappableSyntaxNode, String> nodeValueMapTemplate;
    private Map<MappableSyntaxNode, String> nodeValueMap;
    private Map<SetNode, List<String>> arrayValueMap;

    /**
     * Initialize a new {@link RDFConverter}.
     */
    public RDFConverter(PrefixMapping prefixes) {
        this.prefixes = prefixes;
    }

    private int getCounter(){
        return counter++;
    }

    public List<Model> convertToRDF(CombinedModel combinedModel, List<JsonNode> data) throws ConversionException {
        List<Model> results = new ArrayList<>(data.size());
        for (JsonNode datum : data) {
            results.add(convertToRDF(combinedModel.copy(), datum));
        }
        return results;
    }

    public Model convertToRDF(CombinedModel combinedModel, JsonNode datum) throws ConversionException {
        SemanticModel semanticModel = combinedModel.getSemanticModel();
        if( nodeValueMapTemplate == null || nodeValueMapTemplate.isEmpty()) {
            this.nodeValueMapTemplate = buildValueMapTemplate(combinedModel);
        }
        counter = 0;
        Map<String, RDFNode> uuidtoResource = new HashMap<>();
        // pre-fill the map with all literal values
        buildValueMap(datum);

        String semanticModelId = semanticModel.getId().substring(0, 13);
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(prefixes);
        String smURI = prefixes.getNsPrefixURI("plsm");
        String plasmaURI = prefixes.getNsPrefixURI("plasma");

        Property hasValueProperty = m.createProperty(plasmaURI, "hasValue");

        Map<String, String> uuidValueMap = nodeValueMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getUuid(), Map.Entry::getValue));

        for (SemanticModelNode node : combinedModel.getSemanticModel().getNodes()) {
            RDFNode rdfNode = null;
            if (node instanceof Class) {
                Class clazz = (Class) node;
                Instance i = clazz.getInstance();
                Resource resource;
                if (i != null) {
                    resource =
                            m.createResource(smURI + semanticModelId + "-" + convertToInstanceId(i.getLabel())); // will reuse if already existent
                    resource.addProperty(PLCM.label, m.createLiteral(i.getLabel()));
                    if (i.getDescription() != null && !i.getDescription().isBlank()) {
                        resource.addProperty(PLCM.description, m.createLiteral(i.getDescription()));
                    }
                } else {
                    resource = m.createResource(smURI+ semanticModelId + "-"  + getCounter());
                }

                resource.addProperty(RDF.type, m.createResource(m.expandPrefix(clazz.getURI())));
                if(clazz.isMapped()){
                    String syntaxNodeUuid = clazz.getMappedSyntaxNodeUuid();
                    RDFNode literal;
                    //noinspection Java8MapApi
                    if(!uuidValueMap.containsKey(syntaxNodeUuid)){
                        // throw new ConversionException("Could not find value for syntaxId " + syntaxNodeUuid + " on model " + combinedModel.getId());
                        literal = m.createLiteral(MISSING_DATA_LITERAL);
                    } else {
                        String value = uuidValueMap.get(syntaxNodeUuid);
                        if (ARRAY_LITERAL.equals(value)) {
                            literal = buildNonPrimitiveArray(m, combinedModel, syntaxNodeUuid, datum );
                        } else {
                            literal = m.createLiteral(value);
                        }
                    }
                    m.add(resource,hasValueProperty,literal);
                }
                rdfNode = resource;
            } else if (node instanceof Literal) {
                Literal l = (Literal) node;
                rdfNode = m.createLiteral(l.getValue());
            } else if (node instanceof NamedEntity) {
                NamedEntity ne = (NamedEntity) node;
                // create a resource which describes the named entity
                // this will later be elevated to the ontology
                Resource neResource = m.createResource(m.expandPrefix(ne.getURI()));
                neResource.addProperty(RDF.type, PLCM.NamedEntity);

                Resource resource = m.createResource();
                resource.addProperty(RDF.type, neResource);
                // use the b-node as reference
                rdfNode = resource;
            }
            uuidtoResource.put(node.getUuid(), rdfNode);

        }
        semanticModel.getEdges().forEach(r -> {
            SemanticModelNode from = semanticModel.getNode(r.getFrom());
            SemanticModelNode to = semanticModel.getNode(r.getTo());
            RDFNode fromNode = uuidtoResource.get(from.getUuid());
            RDFNode toNode = uuidtoResource.get(to.getUuid());
            fromNode.asResource().addProperty(m.createProperty(m.expandPrefix(r.getURI())), toNode);
        });
        return m;
    }

    /**
     * Parses the values of an array and converts them to an RDF list
     * @param m The model to add the triples to
     * @param combinedModel The combined model to use
     * @param syntaxNodeUuid The syntax node representing the list
     * @param datum The JSON node containing the data
     * @return The root element of the list
     */
    private RDFNode buildNonPrimitiveArray(Model m, CombinedModel combinedModel, String syntaxNodeUuid, JsonNode datum) throws ConversionException {
        SchemaNode schemaNode = combinedModel.getSyntaxModel().getNode(syntaxNodeUuid);
        if(!(schemaNode instanceof SetNode)){
            throw new ConversionException("Node " + syntaxNodeUuid + " is no SetNode");
        }
        SetNode node = (SetNode) schemaNode;
        // find the corresponding json node
        datum.at(JsonPointer.compile(node.getPath()))
        return null;
    }


    /**
     * Generates the template data storage used for each model conversion.
     * Identifies all mapped {@link MappableSyntaxNode}s and reserves a storage place for the value.
     * This includes {@link PrimitiveNode}s and primitive {@link SetNode}s.
     * Non-primitive {@link SetNode}s are included but not filled later on.
     * @param combinedModel The combined model to use
     * @return The template
     */
    private Map<MappableSyntaxNode, String> buildValueMapTemplate(CombinedModel combinedModel) {
        log.debug("Building new value map template");
        Map<MappableSyntaxNode, String> valueMapTemplate = new HashMap<>();
        // initialize template with all mapped primitive nodes
        combinedModel.getSemanticModel().getNodes().stream()
                .filter(SemanticModelNode::isMapped)
                .map(node -> (MappableSemanticModelNode) node)
                .map(MappableSemanticModelNode::getMappedSyntaxNodeUuid)
                .map(id -> combinedModel.getSyntaxModel().getNode(id))
                .filter(Objects::nonNull)
                .map(node -> (MappableSyntaxNode) node)
                .forEach(mappable -> valueMapTemplate.put(mappable, null));
        return valueMapTemplate;
    }


    public static String convertToInstanceId(String label) {
        String formatted = label.trim().toLowerCase().replace(" ", "_");
        formatted = HtmlUtils.htmlEscape(formatted);
        formatted = formatted.replaceAll("\\[", "_");
        formatted = formatted.replaceAll("\\]", "_");
        formatted = formatted.replaceAll("\\)", "_");
        formatted = formatted.replaceAll("\\(", "_");
        return formatted;
    }

    /**
     * Creates a new {@link #nodeValueMap} using the {@link #nodeValueMapTemplate}.
     * Then, all elements in data are crawled and the values in the {@link #nodeValueMap} filled.
     * This fills all values in the map, except those that are to contain lists / arrays.
     * @param data The datum to extract the values from
     * @throws ConversionException In case the map cannot be built for any reason.
     */
    private void buildValueMap(JsonNode data  ) throws ConversionException {
        List<String> path = new ArrayList<>();
        nodeValueMap = new HashMap<>(nodeValueMapTemplate);
        traverseJsonElement(data, ROOT_ELEMENT_NAME, path);
        // validate that there are no unmapped values
        long notMappedNodesCount = nodeValueMap.values().stream()
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
        Optional<MappableSyntaxNode> match = nodeValueMap.keySet().stream()
                .filter(primitive -> Objects.equals(primitive.getLabel(), label)
                        //       &&                        Objects.equals(primitive.getPath(), path)
                )
                .findFirst();
        if (match.isPresent()) {
            // this is a mapped node
            String value = jsonNode.asText();
            nodeValueMap.put(match.get(), value);
        } else {
            log.debug("ValueNode '{}' {} is not mapped", label, path);
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
        // check if we have a match on the whole array
        Optional<MappableSyntaxNode> match = nodeValueMap.keySet().stream()
                .filter(node -> Objects.equals(node.getLabel(), label)
                        //       &&                        Objects.equals(primitive.getPath(), path)
                )
                .filter(node -> node instanceof SetNode)
                .findFirst();
        if (match.isPresent()) {
            // this is a mapped set node. Convert all content into one literal
            if(!array.isEmpty()){
                if(array.get(0) instanceof ValueNode){
                    // we assume this is all value nodes TODO check?
                    String result = readJSONArray(array);
                    nodeValueMap.put(match.get(), result);
                }
            }
        } else {
            log.debug("SetNode '{}' {} is not mapped", label, path);
            for (JsonNode node : array) {
                traverseJsonElement(node, label + "[]", path);
            }
        }
    }

    /**
     * Serializes a json array into a {@link String}
     * @param arrayNode The node containing the data
     * @return A string of all values divided by the ',' character
     */
    private String readJSONArray(ArrayNode arrayNode) {
        List<String> values = new ArrayList<>();
        arrayNode.elements().forEachRemaining(el -> {
            ValueNode v = (ValueNode) el;
            values.add(v.asText());
        });
        return values.stream()
                .map(v -> {
                    if(!v.startsWith("\"") || ! v.endsWith("\"")){
                        v = "\"" + v + "\"";
                    }
                    return v;
                })
                .collect(Collectors.joining(","));
    }

}
