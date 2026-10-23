package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.buw.tmdt.plasma.datamodel.generators.CombinedModelGenerator.generateResourceURI;
import static de.buw.tmdt.plasma.datamodel.generators.CombinedModelGenerator.getCombinedModel;
import static org.junit.jupiter.api.Assertions.*;

public class CombinedModelSemanticModificationTest {
    /** Classes provide context; mapped values are Literals, connected by DataProperties. */
    private CombinedModel modelWithMappedLiteral() {
        CombinedModel model = getCombinedModel();
        model.getSemanticModel().getNodes().clear();
        model.getSemanticModel().getEdges().clear();
        Class airline = new Class(generateResourceURI("airline"), "airline", "An airline");
        airline.setInstance(new Instance(null, null, "Example airline", "Original instance"));
        Literal code = new Literal("code");
        link(code, syntaxNode(model, "codes1"));
        model.getSemanticModel().getNodes().addAll(List.of(airline, code));
        model.getSemanticModel().getEdges().add(
                new DataProperty(airline.getUuid(), code.getUuid(), generateResourceURI("code")));
        model.validate();
        return model;
    }

    private SyntaxNode syntaxNode(CombinedModel model, String label) {
        return model.getSyntaxModel().getNodes().stream()
                .filter(node -> node.getLabel().equals(label)).findFirst().orElseThrow();
    }

    private void link(Literal literal, SyntaxNode syntax) {
        literal.setMappedSyntaxNodeUuid(syntax.getUuid());
        literal.setMappedSyntaxNodeLabel(syntax.getLabel());
        literal.setMappedSyntaxNodePath(syntax.getPathAsJSONPath());
    }

    private Class airline(CombinedModel model) {
        return (Class) model.getSemanticModel().getNodes().get(0);
    }

    private Literal code(CombinedModel model) {
        return (Literal) model.getSemanticModel().getNodes().get(1);
    }

    private DeltaModification change(List<SemanticNode> nodes, List<Relation> edges) {
        return new DeltaModification("test-change", nodes, edges, null, null, 0.0);
    }

    @Test
    public void testApplyFailsIfFinalized() {
        CombinedModel model = modelWithMappedLiteral();
        model.finalizeModel();
        Class company = new Class(generateResourceURI("company"), "company", "A company");
        assertThrows(UnsupportedOperationException.class,
                () -> model.apply(change(List.of(company), null)));
    }

    @Test
    public void testApplyAddOneEntity() {
        CombinedModel model = modelWithMappedLiteral();
        Class company = new Class(generateResourceURI("company"), "company", "A company");
        model.apply(change(List.of(company), null));
        assertEquals(3, model.getSemanticModel().getNodes().size());
        assertTrue(model.getSemanticModel().getNodes().contains(company));
        assertEquals(1, model.getSemanticModel().getEdges().size());
    }

    @Test
    public void testApplyAddOneEntityAndOneRelation() {
        CombinedModel model = modelWithMappedLiteral();
        Class company = new Class(generateResourceURI("company"), "company", "A company");
        Relation relation = new ObjectProperty(airline(model).getUuid(), company.getUuid(), generateResourceURI("isA"));
        model.apply(change(List.of(company), List.of(relation)));
        assertEquals(3, model.getSemanticModel().getNodes().size());
        assertTrue(model.getSemanticModel().getNodes().contains(company));
        assertEquals(2, model.getSemanticModel().getEdges().size());
        assertTrue(model.getSemanticModel().getEdges().contains(relation));
    }

    @Test
    public void testApplyReplaceOneNode() {
        CombinedModel model = modelWithMappedLiteral();
        Class original = airline(model);
        Class replacement = original.copy();
        replacement.setURI(generateResourceURI("company"));
        replacement.setDescription("Updated class");
        replacement.setInstance(new Instance(null, null, "Updated airline", "Updated instance"));
        model.apply(change(List.of(replacement), null));
        Class actual = (Class) model.getSemanticModel().getNode(original.getUuid());
        assertEquals(original.getUuid(), actual.getUuid());
        assertEquals(generateResourceURI("company"), actual.getURI());
        assertEquals("Updated class", actual.getDescription());
        assertNotNull(actual.getInstance());
        assertEquals("Updated airline", actual.getInstance().getLabel());
        assertEquals("Updated instance", actual.getInstance().getDescription());
        assertEquals(2, model.getSemanticModel().getNodes().size());
        assertEquals(1, model.getSemanticModel().getEdges().size());
    }

    @Test
    public void testApplyMapLiteralToPrimitiveNode() {
        CombinedModel model = modelWithMappedLiteral();
        SyntaxNode time = syntaxNode(model, "time");
        assertTrue(model.generateModelLinkings().stream()
                .noneMatch(mapping -> mapping.getPrimitiveUUID().equals(time.getUuid())));
        Literal literal = new Literal("time");
        link(literal, time);
        Relation relation = new DataProperty(airline(model).getUuid(), literal.getUuid(), generateResourceURI("time"));
        model.apply(change(List.of(literal), List.of(relation)));
        Literal actual = (Literal) model.getSemanticModel().getNode(literal.getUuid());
        assertTrue(actual.isMapped());
        assertEquals(time.getUuid(), actual.getMappedSyntaxNodeUuid());
        assertEquals(time.getLabel(), actual.getMappedSyntaxNodeLabel());
        assertEquals(time.getPathAsJSONPath(), actual.getMappedSyntaxNodePath());
        assertEquals(3, model.getSemanticModel().getNodes().size());
        assertEquals(2, model.getSemanticModel().getEdges().size());
        assertTrue(model.generateModelLinkings().stream().anyMatch(mapping ->
                mapping.getNodeUUID().equals(literal.getUuid()) && mapping.getPrimitiveUUID().equals(time.getUuid())));
    }

    @Test
    public void testRejectsMappingToMissingSyntaxNode() {
        // All generated syntax nodes are linkable; exercise an invalid target explicitly.
        CombinedModel model = modelWithMappedLiteral();
        Literal literal = new Literal("missing");
        literal.setMappedSyntaxNodeUuid("missing-syntax-node");
        literal.setMappedSyntaxNodeLabel("missing");
        literal.setMappedSyntaxNodePath("$.missing");
        assertThrows(CombinedModelIntegrityException.class,
                () -> model.apply(change(List.of(literal), null)));
        assertEquals(2, model.getSemanticModel().getNodes().size());
    }

    @Test
    public void testApplyUnmapLiteralFromSyntaxNode() {
        CombinedModel model = modelWithMappedLiteral();
        Literal original = code(model);
        assertTrue(original.isMapped());
        Literal replacement = original.copy();
        replacement.unmap();
        model.apply(change(List.of(replacement), null));
        Literal actual = (Literal) model.getSemanticModel().getNode(original.getUuid());
        assertEquals(original.getUuid(), actual.getUuid());
        assertFalse(actual.isMapped());
        assertNull(actual.getMappedSyntaxNodeUuid());
        assertNull(actual.getMappedSyntaxNodeLabel());
        assertNull(actual.getMappedSyntaxNodePath());
        assertEquals(2, model.getSemanticModel().getNodes().size());
        assertEquals(1, model.getSemanticModel().getEdges().size());
        assertTrue(model.generateModelLinkings().stream()
                .noneMatch(mapping -> mapping.getNodeUUID().equals(original.getUuid())));
    }

    @Test
    public void testApplyRemoveEntityAndRelation() {
        CombinedModel model = modelWithMappedLiteral();
        Class remaining = airline(model);
        Literal removed = code(model);
        Relation relation = model.getSemanticModel().getEdges().get(0);
        DeltaModification modification = change(List.of(removed), List.of(relation));
        modification.setDeletion(true);
        model.apply(modification);
        assertEquals(List.of(remaining), model.getSemanticModel().getNodes());
        assertTrue(model.getSemanticModel().getEdges().isEmpty());
        assertTrue(model.generateModelLinkings().isEmpty());
    }
}
