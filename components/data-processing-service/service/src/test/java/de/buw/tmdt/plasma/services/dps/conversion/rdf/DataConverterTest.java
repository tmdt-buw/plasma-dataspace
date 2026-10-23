package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class DataConverterTest {
    private AdvancedCombinedModelGenerator generator;

    @BeforeEach
    void setup() {
        generator = new AdvancedCombinedModelGenerator();
    }

    private JsonNode flight() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/flight.json")) {
            assertNotNull(input);
            return new ObjectMapper().readTree(input).get(0);
        }
    }

    private Model convert() throws IOException, ConversionException {
        JsonNode flight;
        try (InputStream input = getClass().getResourceAsStream("/flight.json")) {
            assertNotNull(input);
            flight = new ObjectMapper().readTree(input).get(0);
        }
        PrefixMapping prefixes = new PrefixMappingImpl().withDefaultMappings(PrefixMapping.Standard);
        prefixes.setNsPrefix("local", AdvancedCombinedModelGenerator.namespace);
        prefixes.setNsPrefix("plcm", PLCM.getURI());
        prefixes.setNsPrefix("plsm", "http://plasma.uni-wuppertal.de/sm/");
        prefixes.setNsPrefix("plasma", "http://plasma.uni-wuppertal.de/ontology#");
        return new RDFConverter(prefixes).convertToRDF(generator.getFlightModel(), flight);
    }

    private void assertLiteral(Model model, String predicate, String value) {
        assertTrue(model.contains(null, model.createProperty(AdvancedCombinedModelGenerator.namespace + predicate),
                model.createLiteral(value)), "Missing " + predicate + " = " + value);
    }

    @Test
    void testConversionWithoutMappedList() throws IOException, ConversionException {
        Model model = convert();
        assertLiteral(model, "identifier", "AL0025-20201123-1456-1634");
        assertLiteral(model, "hasName", "Airbus A350 XPB");
        assertLiteral(model, "hasName", "Lufthansa");
    }

    @Test
    void testConversionWithMappedPrimitiveList() throws IOException, ConversionException {
        generator.setMapIdentificationNumbers(true);
        Model model = convert();
        assertLiteral(model, "identifier", "AL0025-20201123-1456-1634");
        assertTrue(model.contains(null, model.createProperty(AdvancedCombinedModelGenerator.namespace + "identNumbers")));
    }

    @Test
    void testConversionWithMappedPrimitiveListInstance() throws IOException, ConversionException {
        generator.setMapIdentificationNumberInstance(true);
        Model model = convert();
        assertLiteral(model, "identNum", "1.24");
        assertLiteral(model, "identNum", "2765");
    }

    @Test
    void testConversionWithMappedStaffInstance() throws IOException, ConversionException {
        generator.setMapVesselInformation(false);
        generator.setMapStaffInstance(true);
        Model model = convert();
        assertLiteral(model, "hasName", "Pilot 1");
        assertLiteral(model, "hasName", "Pilot 2");
        assertLiteral(model, "hasName", "Flight attendant 1");
    }

    @Test
    void testConversionWith2ndLayerArray() throws IOException, ConversionException {
        generator.setMapVesselInformation(false);
        generator.setMapStaffInstance(true);
        generator.setMapTagInstance(true);
        Model model = convert();
        assertLiteral(model, "key", "tag1");
        assertLiteral(model, "key", "tag6");
    }
    @Test
    void nestedRowsRemainAssociatedWithTheirOwnParent() throws Exception {
        generator.setMapStaffInstance(true);
        generator.setMapTagInstance(true);
        Model model = convert();
        String ns = AdvancedCombinedModelGenerator.namespace;
        var pilot = model.listSubjectsWithProperty(model.createProperty(ns + "hasName"), "Pilot 1").next();
        var tags = model.listObjectsOfProperty(pilot, model.createProperty(ns + "tags")).toList();
        assertEquals(2, tags.size());
        var keys = tags.stream().map(t -> t.asResource().getProperty(model.createProperty(ns + "key")).getString()).sorted().toList();
        assertEquals(java.util.List.of("tag1", "tag2"), keys);
        assertEquals(3, model.listSubjectsWithProperty(org.apache.jena.vocabulary.RDF.type, model.createResource(ns + "Staff")).toList().size());
    }

    @Test
    void emptyAndMissingArraysDoNotProducePhantomRows() throws Exception {
        generator.setMapStaffInstance(true);
        var input = (com.fasterxml.jackson.databind.node.ObjectNode) flight();
        input.putArray("staff");
        var converter = new RDFConverter(new PrefixMappingImpl());
        Model model = converter.convertToRDF(generator.getFlightModel(), input);
        assertFalse(model.contains(null, model.createProperty(AdvancedCombinedModelGenerator.namespace + "hasStaff")));
        input.remove("staff");
        assertFalse(converter.convertToRDF(generator.getFlightModel(), input).contains(null,
                model.createProperty(AdvancedCombinedModelGenerator.namespace + "hasStaff")));
    }

    @Test
    void missingValuesDoNotBecomeInventedMeasurements() throws Exception {
        var input = (com.fasterxml.jackson.databind.node.ObjectNode) flight();
        input.putNull("id");
        Model model = new RDFConverter(new PrefixMappingImpl()).convertToRDF(generator.getFlightModel(), input);
        assertFalse(model.contains(null, model.createProperty(AdvancedCombinedModelGenerator.namespace + "identifier")));
    }

    @Test
    void fieldNamesAreLiteralTokensAndNotPathExpressions() throws Exception {
        var combined = generator.getFlightModel();
        var syntax = combined.getSyntaxModel().getNodes().stream().filter(n -> n.getLabel().equals("id")).findFirst().orElseThrow();
        syntax.setPath(java.util.List.of("$", "a.b/~$", ""));
        var input = (com.fasterxml.jackson.databind.node.ObjectNode) flight();
        input.putObject("a.b/~$").put("", "special");
        Model model = new RDFConverter(new PrefixMappingImpl()).convertToRDF(combined, input);
        assertLiteral(model, "identifier", "special");
    }

    @Test
    void converterReuseKeepsRecordsDistinctWithoutLeakingArrayRows() throws Exception {
        generator.setMapStaffInstance(true);
        var combined = generator.getFlightModel();
        combined.getSemanticModel().setId("short");
        var converter = new RDFConverter(new PrefixMappingImpl());
        Model first = converter.convertToRDF(combined, flight());
        var secondInput = (com.fasterxml.jackson.databind.node.ObjectNode) flight();
        secondInput.putArray("staff");
        Model second = converter.convertToRDF(combined, secondInput);
        var firstSubjects = first.listSubjects().toList();
        assertTrue(second.listSubjects().toList().stream().noneMatch(firstSubjects::contains));
        assertFalse(second.contains(null, second.createProperty(AdvancedCombinedModelGenerator.namespace + "hasStaff")));
        assertLiteral(first, "identifier", "AL0025-20201123-1456-1634");
    }

    @Test
    void relativePointerRemovesOnlyTheExplicitPrefix() throws Exception {
        var combined = generator.getFlightModel();
        for (var syntax : combined.getSyntaxModel().getNodes()) {
            var path = new java.util.ArrayList<>(syntax.getPath());
            path.add(1, "records"); path.add(2, "*"); syntax.setPath(path);
        }
        Model model = new RDFConverter(new PrefixMappingImpl()).convertToRDF(combined, flight(), "/records/0");
        assertLiteral(model, "identifier", "AL0025-20201123-1456-1634");
        assertThrows(ConversionException.class, () -> new RDFConverter(new PrefixMappingImpl()).convertToRDF(combined, flight(), "$.records"));
    }

}
