package de.buw.tmdt.plasma.ars.modeling.sm.service.basic;


import de.buw.tmdt.plasma.ars.modeling.sm.service.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.semanticmodel.EntityType;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.syntaxmodel.*;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.syntaxmodel.members.EntityConceptSuggestion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SchemaAnalysisSemanticLabeling {

	private static final Logger logger = LoggerFactory.getLogger(SchemaAnalysisSemanticLabeling.class);

	private List<EntityType> entityTypes;

	private Map<String, EntityConcept> fixMappings = new HashMap<>();

	private final Map<String, List<String>> nodeHistory = new HashMap<>();

	private final Map<EntityType, List<String>> entityTypeExamples = new HashMap<>();

	private Map<String, String> requiredPredecessors;

	public void setFixMappings(Map<String, EntityConcept> fixMappings) {
		this.fixMappings = fixMappings;
	}

	public Map<EntityType, List<String>> getEntityTypeExamples() {
		return Collections.unmodifiableMap(entityTypeExamples);
	}

	public void setRequiredPredecessors(Map<String, String> requiredPredecessors) {
		this.requiredPredecessors = requiredPredecessors;
	}

	public Map<String, List<String>> getNodeHistory() {
		return Collections.unmodifiableMap(nodeHistory);
	}

	public void execute(Node n) throws SchemaAnalysisException {
		this.entityTypes = new ArrayList<>();
		addEntityConcepts("Schema Root", n, new ArrayList<>());
	}

	private void addEntityConcepts(String label, Node node, List<String> path) throws SchemaAnalysisException {
		if (node instanceof SetNode) {
			assert label != null;
			addEntityConcepts((SetNode) node, label, path);
		} else if (node instanceof ObjectNode) {
			addEntityConcepts((ObjectNode) node, label, path);
		} else if (node instanceof PrimitiveNode) {
			assert label != null;
			addEntityConcepts((PrimitiveNode) node, label, path);
		} else if (node instanceof CollisionNode) {
			assert label != null;
			addEntityConcepts((CollisionNode) node, label, path);
		} else if (null == node) {
			logger.debug("NullSchema is not going to get a suggested EntityConcept.");
		} else {
			throw new SchemaAnalysisException("Unknown node type in " + Node.class.getCanonicalName());
		}
	}

	private void addEntityConcepts(@NotNull CollisionNode collisionNode, @NotNull String label, List<String> parentPath) throws SchemaAnalysisException {

		List<String> actualPath = new ArrayList<>(parentPath);
		actualPath.add("*");
		this.nodeHistory.put(collisionNode.getUuid().toString(), actualPath);

		if (collisionNode.getPrimitiveNode() != null) {
			addEntityConcepts(collisionNode.getPrimitiveNode(), label, actualPath);
		}
		if (collisionNode.getObjectNode() != null) {
			addEntityConcepts(collisionNode.getObjectNode(), label, actualPath);
		}
		if (collisionNode.getSetNode() != null) {
			addEntityConcepts(collisionNode.getSetNode(), label, actualPath);
		}
	}

	private void addEntityConcepts(@NotNull ObjectNode objectNode, String label, List<String> parentPath) throws SchemaAnalysisException {
		List<String> actualPath = new ArrayList<>(parentPath);
		actualPath.add(label);
		this.nodeHistory.put(objectNode.getUuid().toString(), actualPath);

		for (Map.Entry<String, Node> child : objectNode.getChildren().entrySet()) {
			addEntityConcepts(child.getKey(), child.getValue(), actualPath);
		}
	}

	private void addEntityConcepts(@NotNull SetNode setNode, @NotNull String label, List<String> parentPath) throws SchemaAnalysisException {
		List<String> actualPath = new ArrayList<>(parentPath);
		actualPath.add("*");
		this.nodeHistory.put(setNode.getUuid().toString(), actualPath);

		for (SetNode.Child n : setNode.getChildren()) {
			addEntityConcepts(label, n.getNode(), actualPath);
		}
	}

	private void addEntityConcepts(@NotNull PrimitiveNode primitiveNode, @NotNull String label, List<String> parentPath) {

		List<String> actualPath = new ArrayList<>(parentPath);
		actualPath.add(label);
		this.nodeHistory.put(primitiveNode.getUuid().toString(), actualPath);

		if (fixMappings.containsKey(label) &&
				(requiredPredecessors.get(fixMappings.get(label).getName()) == null ||
						parentPath.get(parentPath.size() - 1).equals(requiredPredecessors.get(fixMappings.get(label).getName())))) {
			EntityConcept assignedConcept = fixMappings.get(label);
			primitiveNode.getEntityConceptSuggestions().removeAll(primitiveNode.getEntityConceptSuggestions());
			primitiveNode.getEntityConceptSuggestions().add(new EntityConceptSuggestion(assignedConcept, 1.0));
			EntityType entityType = new EntityType(null, label, label, assignedConcept.getDescription(), assignedConcept, null);
			entityTypes.add(entityType);
			primitiveNode.setEntityType(entityType);
			entityTypeExamples.put(entityType, primitiveNode.getExamples());
		} else if (primitiveNode.getEntityConceptSuggestions().size() > 0) {
			double bestWeight = 0;
			EntityConcept bestConcept = null;
			for (EntityConceptSuggestion ecs : primitiveNode.getEntityConceptSuggestions()) {
				if (ecs.getWeight() > bestWeight) {
					bestWeight = ecs.getWeight();
					bestConcept = ecs.getEntityConcept();
				}
			}
			if (bestConcept == null) {
				bestConcept = primitiveNode.getEntityConceptSuggestions().get(0).getEntityConcept();
			}
			EntityType entityType = new EntityType(null, bestConcept.getName(), label, bestConcept.getDescription(), bestConcept, null);
			entityTypes.add(entityType);
			primitiveNode.setEntityType(entityType);
			entityTypeExamples.put(entityType, primitiveNode.getExamples());
		}
	}

	public List<EntityType> getEntityTypes() {
		return entityTypes;
	}

}
