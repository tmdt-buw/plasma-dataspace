package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static de.buw.tmdt.plasma.datamodel.CombinedModelGenerator.getCombinedModel;
import static org.junit.jupiter.api.Assertions.*;

public class CombinedModelSyntacticModificationTest {

    @Test
    public void testSplitPrimitiveNode() {
        CombinedModel model = getCombinedModel();

        SchemaNode targetNode = model.getSyntaxModel().getNodes().stream().filter(schemaNode -> schemaNode.getLabel().equals("ident")).findFirst().orElseThrow();

        assertTrue(targetNode instanceof PrimitiveNode);

        PrimitiveNode identNode = (PrimitiveNode) targetNode;

        Splitting splitting1 = new Splitting("-");

        // by just defining the split, needed new nodes should be generated automatically
        CompositeNode compositeNode = new CompositeNode(identNode.getUuid(), "", identNode.getXCoordinate(), identNode.getYCoordinate(), new ArrayList<>(), true, new ArrayList<>(), identNode.getCleansingPattern(), Arrays.asList(splitting1));

        DeltaModification modification = new DeltaModification("mod_testSplitPrimitiveNode", null, null, Collections.singleton(compositeNode), null, 0.0);

        model.apply(modification);

        // assert that the node has been properly replaced
        SchemaNode replacementCompositeNode = model.getSyntaxModel().getNodes().stream().filter(schemaNode -> schemaNode.getUuid().equals(targetNode.getUuid())).findFirst().orElseThrow();

        assertTrue(replacementCompositeNode instanceof CompositeNode, "New node has wrong type");
        assertEquals(1, ((CompositeNode) replacementCompositeNode).getSplitter().size(), "Splitting is missing in new node");
        assertEquals(((PrimitiveNode) targetNode).getCleansingPattern(), ((CompositeNode) replacementCompositeNode).getCleansingPattern(), "Cleansing patterns do not match");


        // assure that no entity types have been deleted
        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        int expectedNewEdgesCount = compositeNode.getSplitter().size() + 1;
        int expectedNewNodesCount = compositeNode.getSplitter().size() + 1;
        // assert that two more nodes and two more edges have been created in the syntactic model
        assertEquals(getCombinedModel().getSyntaxModel().getNodes().size() + expectedNewNodesCount, model.getSyntaxModel().getNodes().size(), "Number of expected nodes does not match");
        assertEquals(getCombinedModel().getSyntaxModel().getEdges().size() + expectedNewEdgesCount, model.getSyntaxModel().getEdges().size(), "Number of expected edges does not match");


        // assert that the originally assigned ET is disconnected now, as the node is no longer a PrimitiveNode
        EntityType identifierType = model.getSemanticModel().getNodes().stream().filter(entityType -> entityType.getEntityConcept().getSourceURI().equals("plasma:identifier")).findFirst().orElseThrow();
        assertNull(identifierType.getOriginalLabel());
        assertNull(identifierType.getSyntaxNodeUuid());

        // also assure that the explicit mapping has been removed
        assertTrue(model.getPrimitiveEntityTypeMappings().stream().noneMatch(primitiveEntityTypeMapping -> primitiveEntityTypeMapping.getPrimitiveUUID().equals(identNode.getUuid())), "Explicit mapping to old node has not been removed");

        // get outgoing edges
        List<Edge> newEdges = model.getSyntaxModel().getEdges().stream().filter(edge -> edge.getFrom().equals(compositeNode.getUuid())).collect(Collectors.toList());

        assertEquals(expectedNewEdgesCount, newEdges.size(), "Missing created edges or edges not properly instantiated");

        // get the generated nodes
        List<String> generatedNodesUuids = newEdges.stream().map(Edge::getTo).collect(Collectors.toList());
        List<PrimitiveNode> generatedNodes = model.getSyntaxModel().getNodes().stream()
                .filter(schemaNode -> generatedNodesUuids.contains(schemaNode.getUuid()))
                .map(schemaNode -> (PrimitiveNode) schemaNode)
                .sorted(Comparator.comparing(CombinedModelElement::getLabel))
                .collect(Collectors.toList());

        // validate example values of generated nodes
        assertTrue(generatedNodes.get(0).getExamples().containsAll(Arrays.asList("10", "20", "test")), "First generated nodes example values do not match");
        assertTrue(generatedNodes.get(1).getExamples().containsAll(Arrays.asList("11", "21", "test")), "Second generated nodes example values do not match");

        // TODO assert that example values, coordinates and cleansing pattern are correctly copied to composite node
        // TODO assert that a potential m apping to the old primitive node is detached!
    }

    // TODO add test re-split

}