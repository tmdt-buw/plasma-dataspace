package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode.ARRAY_PATH_TOKEN;
import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode.PREDEFINED_LABELS;
import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode.ROOT_PATH_TOKEN;

/** Converts mapped values and their array contexts without interpreting field names as JSONPath. */
public class RDFConverter {
    private final PrefixMapping prefixes;
    // Keep identifiers distinct when the caller merges several converted records.
    private long counter;

    public RDFConverter(PrefixMapping prefixes) {
        this.prefixes = PrefixMapping.Factory.create()
                .setNsPrefix("plsm", "http://plasma.uni-wuppertal.de/sm/")
                .setNsPrefix("plasma", "http://plasma.uni-wuppertal.de/ontology#")
                .setNsPrefix("plcm", PLCM.getURI())
                .setNsPrefixes(prefixes);
    }

    /** Conversion state is local to each call; retained for callers of the former stateful converter. */
    public void reset() { }

    public List<Model> convertToRDF(CombinedModel model, List<JsonNode> data) throws ConversionException {
        return convertToRDF(model, data, null);
    }

    public List<Model> convertToRDF(CombinedModel model, List<JsonNode> data, String relativePath) throws ConversionException {
        List<Model> results = new ArrayList<>(data.size());
        for (JsonNode datum : data) results.add(convertToRDF(model, datum, relativePath));
        return results;
    }

    public Model convertToRDF(CombinedModel model, JsonNode datum) throws ConversionException {
        return convertToRDF(model, datum, null);
    }

    /** relativePath, when present, is the JSON Pointer locating this datum in the original document. */
    public Model convertToRDF(CombinedModel model, JsonNode datum, String relativePath) throws ConversionException {
        if (model == null || model.getSemanticModel() == null || datum == null) {
            throw new ConversionException("A semantic model and input datum are required");
        }
        Model result = ModelFactory.createDefaultModel().setNsPrefixes(prefixes);
        SemanticModel semantic = model.getSemanticModel();
        Map<String, List<String>> paths = new HashMap<>();
        Map<String, List<String>> scopes = new HashMap<>();
        List<String> base = pointerTokens(relativePath);
        for (SemanticNode node : semantic.getNodes()) {
            List<String> path = null;
            if (node instanceof LinkableSemanticNode linked && linked.isLinked()) {
                SyntaxNode syntax = model.getSyntaxModel().getNode(linked.getMappedSyntaxNodeUuid());
                if (syntax == null) throw new ConversionException("Missing syntax node for " + node.getUuid());
                path = relativeTokens(syntax.getPath(), base);
                paths.put(node.getUuid(), path);
            }
            scopes.put(node.getUuid(), arrayScope(path));
        }
        // An unlinked class or constant in an array context must be instantiated for each row too.
        Set<String> visited = new HashSet<>();
        for (SemanticNode node : semantic.getNodes()) {
            if (visited.contains(node.getUuid())) continue;
            SemanticModel context = semantic.getArrayContext(node, new ArrayList<>());
            List<String> scope = List.of();
            for (SemanticNode member : context.getNodes()) {
                List<String> candidate = scopes.get(member.getUuid());
                if (!scope.isEmpty() && !candidate.isEmpty() && !scope.equals(candidate)) {
                    throw new ConversionException("An array-context relation joins incompatible array scopes");
                }
                if (!candidate.isEmpty()) scope = candidate;
            }
            for (SemanticNode member : context.getNodes()) {
                scopes.put(member.getUuid(), scope);
                visited.add(member.getUuid());
            }
        }

        String id = semantic.getId() == null ? UUID.randomUUID().toString() : semantic.getId();
        String baseUri = prefixes.getNsPrefixURI("plsm") + id.substring(0, Math.min(13, id.length())) + "-";
        Map<String, List<Occurrence>> occurrences = new HashMap<>();
        for (SemanticNode node : semantic.getNodes()) {
            List<Map<List<String>, Integer>> bindings = new ArrayList<>();
            enumerate(datum, scopes.get(node.getUuid()), 0, new LinkedHashMap<>(), bindings);
            List<Occurrence> values = new ArrayList<>();
            for (Map<List<String>, Integer> binding : bindings) {
                JsonNode value = paths.containsKey(node.getUuid()) ? resolve(datum, paths.get(node.getUuid()), binding) : null;
                RDFNode rdf = convertNode(result, node, value, baseUri);
                if (rdf != null) values.add(new Occurrence(rdf, binding));
            }
            occurrences.put(node.getUuid(), values);
        }
        for (Relation relation : semantic.getEdges()) {
            List<Occurrence> subjects = occurrences.get(relation.getFrom());
            List<Occurrence> objects = occurrences.get(relation.getTo());
            if (subjects == null || objects == null) throw new ConversionException("Relation references a missing semantic node");
            Property predicate = result.createProperty(result.expandPrefix(relation.getURI()));
            for (Occurrence subject : subjects) {
                if (!subject.value.isResource()) throw new ConversionException("A literal cannot be the subject of an RDF relation");
                for (Occurrence object : objects) {
                    if (compatible(subject.indices, object.indices)) subject.value.asResource().addProperty(predicate, object.value);
                }
            }
        }
        return result;
    }

    private RDFNode convertNode(Model result, SemanticNode node, JsonNode value, String baseUri) throws ConversionException {
        if (node instanceof Class clazz) {
            // Class links describe the row's context. Only Literal mappings produce data values.
            Resource resource = result.createResource(baseUri + counter++);
            resource.addProperty(RDF.type, result.createResource(result.expandPrefix(clazz.getURI())));
            Instance instance = clazz.getInstance();
            if (instance != null) {
                if (instance.getLabel() != null && !PREDEFINED_LABELS.contains(instance.getLabel())) {
                    resource.addProperty(PLCM.label, instance.getLabel());
                }
                if (instance.getDescription() != null && !instance.getDescription().isBlank()) {
                    resource.addProperty(PLCM.description, instance.getDescription());
                }
            }
            return resource;
        }
        if (node instanceof de.buw.tmdt.plasma.datamodel.semanticmodel.Literal literal) {
            if (!literal.isMapped()) return literal.getValue() == null ? null : result.createLiteral(literal.getValue());
            // Missing measurements do not become fabricated strings or values.
            if (value == null || value.isMissingNode() || value.isNull()) return null;
            if (value.isObject()) throw new ConversionException("An object must be linked to a Class, not mapped to a Literal");
            return result.createLiteral(value.isArray() ? value.toString() : value.asText());
        }
        if (node instanceof NamedEntity named) {
            Resource type = result.createResource(result.expandPrefix(named.getURI()));
            type.addProperty(RDF.type, PLCM.NamedEntity);
            return result.createResource().addProperty(RDF.type, type);
        }
        throw new ConversionException("Unsupported semantic node " + node.getClass().getSimpleName());
    }

    private record Occurrence(RDFNode value, Map<List<String>, Integer> indices) { }

    private static boolean compatible(Map<List<String>, Integer> first, Map<List<String>, Integer> second) {
        return first.entrySet().stream().allMatch(entry -> !second.containsKey(entry.getKey()) || second.get(entry.getKey()).equals(entry.getValue()));
    }

    private static List<String> arrayScope(List<String> path) {
        if (path == null) return List.of();
        int last = path.lastIndexOf(ARRAY_PATH_TOKEN);
        return last < 0 ? List.of() : List.copyOf(path.subList(0, last + 1));
    }

    private static void enumerate(JsonNode datum, List<String> path, int offset,
                                  Map<List<String>, Integer> binding, List<Map<List<String>, Integer>> results) throws ConversionException {
        if (offset == path.size()) { results.add(Map.copyOf(binding)); return; }
        if (datum == null || datum.isMissingNode() || datum.isNull()) return;
        String token = path.get(offset);
        if (ARRAY_PATH_TOKEN.equals(token)) {
            if (!datum.isArray()) throw new ConversionException("Expected an array at " + path.subList(0, offset));
            List<String> key = List.copyOf(path.subList(0, offset + 1));
            for (int index = 0; index < datum.size(); index++) {
                binding.put(key, index);
                enumerate(datum.get(index), path, offset + 1, binding, results);
            }
            binding.remove(key);
        } else {
            enumerate(datum.path(token), path, offset + 1, binding, results);
        }
    }

    private static JsonNode resolve(JsonNode datum, List<String> path, Map<List<String>, Integer> binding) {
        JsonNode value = datum;
        for (int offset = 0; offset < path.size(); offset++) {
            if (ARRAY_PATH_TOKEN.equals(path.get(offset))) {
                Integer index = binding.get(path.subList(0, offset + 1));
                if (index == null) return null;
                value = value.path(index);
            } else value = value.path(path.get(offset));
        }
        return value;
    }

    private static List<String> relativeTokens(List<String> path, List<String> base) throws ConversionException {
        List<String> tokens = new ArrayList<>(path);
        if (!tokens.isEmpty() && ROOT_PATH_TOKEN.equals(tokens.get(0))) tokens.remove(0);
        if (base.size() > tokens.size()) throw new ConversionException("Relative path lies outside the mapped path");
        for (int i = 0; i < base.size(); i++) {
            if (!tokens.get(i).equals(base.get(i)) && !ARRAY_PATH_TOKEN.equals(tokens.get(i))) {
                throw new ConversionException("Relative path does not match the mapped path");
            }
        }
        return List.copyOf(tokens.subList(base.size(), tokens.size()));
    }

    private static List<String> pointerTokens(String pointer) throws ConversionException {
        List<String> tokens = new ArrayList<>();
        try {
            for (JsonPointer part = JsonPointer.compile(pointer == null ? "" : pointer); !part.matches(); part = part.tail()) {
                tokens.add(part.getMatchingProperty());
            }
        } catch (IllegalArgumentException exception) {
            throw new ConversionException("Relative path must be a JSON Pointer", exception);
        }
        return tokens;
    }

    public static String convertToInstanceId(String label) {
        return HtmlUtils.htmlEscape(label.trim().toLowerCase(Locale.ROOT).replace(" ", "_"))
                .replace("##", "_").replaceAll("[#\\[\\]()]", "_").replace("__", "_");
    }
}
