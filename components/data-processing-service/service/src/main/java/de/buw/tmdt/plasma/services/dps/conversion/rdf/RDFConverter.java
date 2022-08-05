package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.jetbrains.annotations.NotNull;
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
    private static final String ROOT_ELEMENT_NAME = "";
    private static final String ARRAY_ELEMENT_NAME = "0";

    private static final String MISSING_DATA_LITERAL = "##ValueNotAvailable##";
    private static final String ARRAY_LITERAL = "##Array##";

    private final PrefixMapping prefixes;
    private Map<MappableSyntaxNode, String> nodeValueMapTemplate;
    private Map<SetNode, List<String>> arrayValueMap;

    /* Runtime resources */
    private CombinedModel combinedModel;
    private JsonNode datum;
    private Map<String, RDFNode> uuidtoResource = new HashMap<>();
    private Model m;
    private String smURI;
    private String plasmaURI;
    private Property hasValueProperty;
    private String semanticModelId;
    //private Map<String, String> uuidValueMap = new HashMap<>();
    // private Map<MappableSyntaxNode, String> nodeValueMap = new HashMap<>();
    private Map<SemanticModelNode, List<RDFNode>> arrayExpansionMap = new HashMap<>();
    private int counter = 0;
    private boolean dirty = false;

    /**
     * Initialize a new {@link RDFConverter}.
     */
    public RDFConverter(PrefixMapping prefixes) {
        this.prefixes = prefixes;
    }

    public void reset() {
        uuidtoResource = new HashMap<>();
        //uuidValueMap = new HashMap<>();
        arrayExpansionMap = new HashMap<>();

        semanticModelId = null;
        combinedModel = null;
        datum = null;
        dirty = false;
    }

    private void validate() {
        // TODO
    }

    private int getCounter() {
        return counter++;
    }

    public List<Model> convertToRDF(CombinedModel combinedModel, List<JsonNode> data) throws ConversionException {
        List<Model> results = new ArrayList<>(data.size());
        for (JsonNode datum : data) {
            results.add(convertToRDF(combinedModel, datum));
        }
        return results;
    }

    public Model convertToRDF(CombinedModel combinedModel, JsonNode datum) throws ConversionException {
        SemanticModel semanticModel = combinedModel.getSemanticModel();
        if (nodeValueMapTemplate == null || nodeValueMapTemplate.isEmpty()) {
            this.nodeValueMapTemplate = buildValueMapTemplate(combinedModel);
        }
        if (dirty) {
            reset();
        }
        dirty = true;
        this.combinedModel = combinedModel;
        this.datum = datum;

        // pre-fill the map with all literal values
        //buildValueMap(datum);

        semanticModelId = semanticModel.getId().substring(0, 13);
        m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(prefixes);
        smURI = prefixes.getNsPrefixURI("plsm");
        plasmaURI = prefixes.getNsPrefixURI("plasma");
        hasValueProperty = m.createProperty(plasmaURI, "hasValue");
        //uuidValueMap = nodeValueMap.entrySet().stream()
        //        .collect(Collectors.toMap(entry -> entry.getKey().getUuid(), Map.Entry::getValue));

        for (SemanticModelNode node : combinedModel.getSemanticModel().getNodes()) {
            convertNodeToRDF(node, false);

        }
        for (Relation r : semanticModel.getEdges()) {
            SemanticModelNode from = semanticModel.getNode(r.getFrom());
            SemanticModelNode to = semanticModel.getNode(r.getTo());
            RDFNode fromNode = uuidtoResource.get(from.getUuid());
            RDFNode toNode = uuidtoResource.get(to.getUuid());
            if(arrayExpansionMap.containsKey(to)){
                List<RDFNode> rdfNodes = arrayExpansionMap.get(to);
                for (RDFNode expandedNode : rdfNodes) {
                    fromNode.asResource().addProperty(m.createProperty(m.expandPrefix(r.getURI())), expandedNode);
                }
            } else {
                fromNode.asResource().addProperty(m.createProperty(m.expandPrefix(r.getURI())), toNode);
            }

        }

        return m;
    }

    private void convertNodeToRDF(SemanticModelNode node, boolean force) throws ConversionException {
        if (uuidtoResource.containsKey(node.getUuid()) && !force) {
            return;
        }
        RDFNode rdfNode = null;
        if (node instanceof Class) {
            Class clazz = (Class) node;
            if (clazz.isMapped()) {
                String syntaxNodeUuid = clazz.getMappedSyntaxNodeUuid();
                RDFNode resultNode;

                SchemaNode schemaNode = combinedModel.getSyntaxModel().getNode(syntaxNodeUuid);
                List<String> nodePath = schemaNode.getPath();
                if (ARRAY_ELEMENT_NAME.equals(nodePath.get(nodePath.size() - 1))) {
                    // this is a child node of an array, refer back to parent node to build list
                    JsonNode arrayNode = datum.at(JsonPointer.compile(schemaNode.getPathAsJSONPointer()).head());
                    if (!(arrayNode instanceof ArrayNode)) {
                        throw new ConversionException("Node " + JsonPointer.compile(schemaNode.getPathAsJSONPointer()).head() + " is not an array node!");
                    }
                    RDFList list = buildRDFListFromArray(arrayNode, clazz);
                    arrayExpansionMap.put(clazz, list.asJavaList());
                    resultNode = list;
                } else if (schemaNode instanceof SetNode){
                    JsonNode arrayNode = datum.at(JsonPointer.compile(schemaNode.getPathAsJSONPointer()));
                    if (!(arrayNode instanceof ArrayNode)) {
                        throw new ConversionException("Node " + JsonPointer.compile(schemaNode.getPathAsJSONPointer()) + " is not an array node!");
                    }
                    String value = readJSONArray((ArrayNode) arrayNode);
                    resultNode = m.createLiteral(value);
                }
                else {
                    //String value = uuidValueMap.get(syntaxNodeUuid);
                    JsonNode valueNode = datum.at(JsonPointer.compile(schemaNode.getPathAsJSONPointer()));
                    if(!valueNode.isValueNode()){
                        throw new ConversionException("Node " + JsonPointer.compile(schemaNode.getPathAsJSONPointer()) + " is not a value node!");
                    }
                    Resource resource = convertClassNodeToRDF(clazz);
                    org.apache.jena.rdf.model.Literal valueLiteral;
                    if(valueNode.isMissingNode()){
                        // throw new ConversionException("Could not find value for syntaxId " + syntaxNodeUuid + " on model " + combinedModel.getId());
                        valueLiteral = m.createLiteral(MISSING_DATA_LITERAL);
                    } else {
                        String value = valueNode.textValue();
                        valueLiteral = m.createLiteral(value);
                    }
                    m.add(resource, hasValueProperty, valueLiteral);
                    resultNode = resource;
                }
                rdfNode = resultNode;
            } else {
                // not mapped
                Resource resource = convertClassNodeToRDF(clazz);
                rdfNode = resource;
            }
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

    @NotNull
    private Resource convertClassNodeToRDF(Class clazz){
        Instance i = clazz.getInstance();
        Resource resource;
        String baseURI = smURI + semanticModelId + "-" + getCounter();
        if (i != null) {
            resource =
                    m.createResource(baseURI + "-" + convertToInstanceId(i.getLabel())); // will reuse if already existent
            resource.addProperty(PLCM.label, m.createLiteral(i.getLabel()));
            if (i.getDescription() != null && !i.getDescription().isBlank()) {
                resource.addProperty(PLCM.description, m.createLiteral(i.getDescription()));
            }
        } else {
            resource = m.createResource(baseURI);
        }
        resource.addProperty(RDF.type, m.createResource(m.expandPrefix(clazz.getURI())));
        return resource;
    }

    /**
     * Parses the values of an array and converts them to an {@link RDFList}
     *
     * @param arrayNode The JSON node containing the array data
     * @return The root element of the list
     */
    private RDFList buildRDFListFromArray(JsonNode arrayNode, Class clazz) throws ConversionException {
        /*
        SchemaNode schemaNode = combinedModel.getSyntaxModel().getNode(syntaxNodeUuid);
        if(!(schemaNode instanceof SetNode)){
            throw new ConversionException("Node " + syntaxNodeUuid + " is no SetNode");
        }
        SetNode node = (SetNode) schemaNode;
         */
        List<RDFNode> nodes = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            if (node.isValueNode()) {
                if(clazz !=null){
                    Resource classResource = convertClassNodeToRDF(clazz);
                    org.apache.jena.rdf.model.Literal valueLiteral = m.createLiteral(node.asText());
                    classResource.addProperty(hasValueProperty,valueLiteral);
                    nodes.add(classResource);
                } else {
                    nodes.add(m.createLiteral(node.asText()));
                }

            } else {
                // skipped for now
            }
        }
        return m.createList(nodes.iterator());
    }


    /**
     * Generates the template data storage used for each model conversion.
     * Identifies all mapped {@link MappableSyntaxNode}s and reserves a storage place for the value.
     * This includes {@link PrimitiveNode}s and primitive {@link SetNode}s.
     * Non-primitive {@link SetNode}s are included but not filled later on.
     *
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
     * Serializes a json array into a {@link String}
     *
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
                    if (!v.startsWith("\"") || !v.endsWith("\"")) {
                        v = "\"" + v + "\"";
                    }
                    return v;
                })
                .collect(Collectors.joining(","));
    }

}
