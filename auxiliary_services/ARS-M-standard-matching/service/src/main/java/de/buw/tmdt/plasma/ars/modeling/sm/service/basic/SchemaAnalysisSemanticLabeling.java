package de.buw.tmdt.plasma.ars.modeling.sm.service.basic;


import de.buw.tmdt.plasma.ars.modeling.sm.service.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SchemaAnalysisSemanticLabeling {

    private static final Logger logger = LoggerFactory.getLogger(SchemaAnalysisSemanticLabeling.class);
    private final Map<String, List<String>> nodePaths = new HashMap<>();
    // storage entity to hold all newly created entities and concepts
    private SemanticModel semanticModel;
    private SyntaxModel syntaxModel;
    private Map<String, EntityConcept> fixedRulebasedMappings = new HashMap<>();
    private Map<String, String> requiredPredecessors;

    public void setFixedRulebasedMappings(Map<String, EntityConcept> fixedRulebasedMappings) {
        this.fixedRulebasedMappings = fixedRulebasedMappings;
    }

    public void setRequiredPredecessors(Map<String, String> requiredPredecessors) {
        this.requiredPredecessors = requiredPredecessors;
    }

    public Map<String, List<String>> getNodePaths() {
        return Collections.unmodifiableMap(nodePaths);
    }

    public void execute(CombinedModel combinedModel) throws SchemaAnalysisException {
        this.syntaxModel = combinedModel.getSyntaxModel();
        this.semanticModel = new SemanticModel();

        // get root node and start depth first assignment
        SchemaNode root = syntaxModel.getNode(syntaxModel.getRoot());
        addEntityConcepts(root, new ArrayList<>());
    }

    private void addEntityConcepts(SchemaNode node, List<String> path) throws SchemaAnalysisException {
        if (node instanceof SetNode) {
            addEntityConcepts((SetNode) node, path);
        } else if (node instanceof ObjectNode) {
            addEntityConcepts((ObjectNode) node, path);
        } else if (node instanceof PrimitiveNode) {
            addEntityConcepts((PrimitiveNode) node, path);
        } else if (node instanceof CollisionSchema) {
            addEntityConcepts((CollisionSchema) node, path);
        } else if (null == node) {
            logger.debug("NullSchema is not going to get a suggested EntityConcept.");
        } else {
            throw new SchemaAnalysisException("Unknown node type in " + SchemaNode.class.getCanonicalName());
        }
    }

    private void addEntityConcepts(@NotNull CollisionSchema collisionNode, List<String> parentPath) throws SchemaAnalysisException {

        List<String> actualPath = new ArrayList<>(parentPath);
        actualPath.add("*");
        this.nodePaths.put(collisionNode.getUuid(), actualPath);

        List<SchemaNode> childNodes = syntaxModel.getChildNodesForNode(collisionNode.getUuid());
        for (SchemaNode child : childNodes) {
            addEntityConcepts(child, actualPath);
        }
    }

    private void addEntityConcepts(@NotNull ObjectNode objectNode, List<String> parentPath) throws SchemaAnalysisException {
        List<String> actualPath = new ArrayList<>(parentPath);
        actualPath.add(objectNode.getLabel());
        this.nodePaths.put(objectNode.getUuid(), actualPath);

        List<SchemaNode> childNodes = syntaxModel.getChildNodesForNode(objectNode.getUuid());
        for (SchemaNode child : childNodes) {
            addEntityConcepts(child, actualPath);
        }
    }

    private void addEntityConcepts(@NotNull SetNode setNode, List<String> parentPath) throws SchemaAnalysisException {
        List<String> actualPath = new ArrayList<>(parentPath);
        actualPath.add(setNode.getLabel());
        this.nodePaths.put(setNode.getUuid(), actualPath);

        List<SchemaNode> childNodes = syntaxModel.getChildNodesForNode(setNode.getUuid());
        for (SchemaNode child : childNodes) {
            addEntityConcepts(child, actualPath);
        }
    }

    private void addEntityConcepts(@NotNull PrimitiveNode primitiveNode, List<String> parentPath) {

        List<String> actualPath = new ArrayList<>(parentPath);
        actualPath.add(primitiveNode.getLabel());
        this.nodePaths.put(primitiveNode.getUuid(), actualPath);
        String label = primitiveNode.getLabel();

        if (fixedRulebasedMappings.containsKey(label)) {
            EntityConcept concept = fixedRulebasedMappings.get(label);
            String requiredPredecessor = requiredPredecessors.get(concept.getLabel());
            if (requiredPredecessor == null ||
                    requiredPredecessor.equals(parentPath.get(parentPath.size() - 1))) {
                EntityType entityType = new EntityType(
                        primitiveNode.getLabel(),
                        primitiveNode.getXCoordinate(),
                        primitiveNode.getYCoordinate(),
                        primitiveNode.getUuid(),
                        primitiveNode.getLabel(),
                        null,
                        concept);
                semanticModel.getNodes().add(entityType);
            }
        }

    }

    public SemanticModel getSemanticModel() {
        return semanticModel;
    }
}
