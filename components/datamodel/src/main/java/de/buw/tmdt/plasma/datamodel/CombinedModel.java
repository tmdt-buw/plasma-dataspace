package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.Pair;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The combined model contains all elements that model the current state of the syntactic
 * and semantic model of a data source.
 */
public class CombinedModel implements Serializable {

	public static final String SYNTAX_MODEL_PROPERTY = "syntaxModel";
	public static final String SEMANTIC_MODEL_PROPERTY = "semanticModel";
	private static final String PRIMITIVE_ENTITY_TYPE_LIST = "primitiveToEntityTypeList";
	private static final String RECOMMENDATION_LIST = "recommendations";
	private static final String FINALIZED = "finalized";
	private static final long serialVersionUID = 1069167546126071914L;

	private final SyntaxModel syntaxModel;
	private final SemanticModel semanticModel;
	private final ArrayList<PrimitiveEntityTypeMapping> primitiveEntityTypeMappings;
	/**
	 * Recommendations represent suggested modifications for the current state of models
	 */
	private ArrayList<DeltaModification> recommendations;
	/**
	 * A finalized model should not be changed any more
	 */
	private boolean finalized;

	public CombinedModel(
			@JsonProperty(SYNTAX_MODEL_PROPERTY) @NotNull SyntaxModel syntaxModel,
			@JsonProperty(SEMANTIC_MODEL_PROPERTY) SemanticModel semanticModel,
			@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST) List<? extends PrimitiveEntityTypeMapping> primitiveEntityTypeMappings,
			@JsonProperty(RECOMMENDATION_LIST) List<DeltaModification> recommendations,
			@JsonProperty(value = FINALIZED, defaultValue = "false") boolean finalized
	) {
		this.syntaxModel = syntaxModel;
		this.semanticModel = semanticModel == null ? new SemanticModel(null, null) : semanticModel;
		this.primitiveEntityTypeMappings = primitiveEntityTypeMappings == null ? new ArrayList<>() : new ArrayList<>(primitiveEntityTypeMappings);
		this.finalized = finalized;
		this.recommendations = recommendations == null ? new ArrayList<>() : new ArrayList<>(recommendations);
	}

	@JsonProperty(SYNTAX_MODEL_PROPERTY)
	public SyntaxModel getSyntaxModel() {
		return syntaxModel;
	}

	@JsonProperty(SEMANTIC_MODEL_PROPERTY)
	public SemanticModel getSemanticModel() {
		return semanticModel;
	}

	@JsonProperty(value = FINALIZED, defaultValue = "false")
	public boolean isFinalized() {
		return finalized;
	}

	@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST)
	public List<PrimitiveEntityTypeMapping> getPrimitiveEntityTypeMappings() {
		return primitiveEntityTypeMappings;
	}

	@NotNull
	@JsonProperty(RECOMMENDATION_LIST)
	public ArrayList<DeltaModification> getRecommendations() {
		return recommendations;
	}

	public void finalizeModel() {
		this.finalized = true;
	}

	// we allow later injections of recommendations, will be solved with new data model
	public void setRecommendations(ArrayList<DeltaModification> recommendations) {
		this.recommendations = recommendations;
	}

	public void apply(DeltaModification delta) {
		if (finalized) {
			throw new UnsupportedOperationException("Model is finalized. No more modifications possible");
		}
		// apply syntactic modifications
		// update nodes
		List<SchemaNode> nodesToBeReplaced = new ArrayList<>();
		if (CollectionUtilities.hasElements(delta.getNodes())) {
			if (delta.isDeletion()) {
				throw new UnsupportedOperationException("Cannot remove syntax nodes");
			}
			// as we match by uuid only, we can use removeAll and addAll
			// remove all nodes that are to be overwritten (i.e. uuids match)
			nodesToBeReplaced = syntaxModel.getNodes().stream().filter(schemaNode -> delta.getNodes().contains(schemaNode)).collect(Collectors.toList());
			syntaxModel.getNodes().removeAll(nodesToBeReplaced);


			// now add the new ones with the same uuids but new content
			syntaxModel.getNodes().addAll(delta.getNodes());
		}
		// update edges
		if (CollectionUtilities.hasElements(delta.getEdges())) {
			if (delta.isDeletion()) {
				throw new UnsupportedOperationException("Cannot remove syntax nodes");
			}
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			syntaxModel.getEdges().removeAll(delta.getEdges());
			// now add the new ones with the same uuids but new content
			syntaxModel.getEdges().addAll(delta.getEdges());
		}

		// check if any additional syntactic modifications have to be done, e.g. after splitting operation
		if (!delta.isDeletion()) { // cannot delete from syntax model
			// check if he have a split going on
			List<CompositeNode> compositeNodes = delta.getNodes().stream().filter(schemaNode -> schemaNode instanceof CompositeNode).map(schemaNode -> (CompositeNode) schemaNode).collect(Collectors.toList());
			for (CompositeNode compositeNode : compositeNodes) {
				// get the replaced node
				SchemaNode replacedNode = nodesToBeReplaced.stream().filter(schemaNode -> schemaNode.equals(compositeNode)).findFirst().orElse(null);
				if (replacedNode == null) {
					// nothing to be done here, something has gone wrong
					continue;
				}
				if (replacedNode instanceof CompositeNode) {
					// check if the patterns match, as this was split before
					// TODO
				}
				if (replacedNode instanceof PrimitiveNode) {
					// we split a previous primitive node
					PrimitiveNode replaced = (PrimitiveNode) replacedNode;
					List<Splitting> splitter = compositeNode.getSplitter();

					// generate new nodes
					List<PrimitiveNode> components = new ArrayList<>(splitter.size() + 1);
					for (int i = 0; i < splitter.size() + 1; i++) {
						components.add(new PrimitiveNode(replaced.getLabel() + "-" + i, replaced.getOperations(), true, DataType.UNKNOWN.name(), new ArrayList<>(), null));
					}

					// split example values
					for (String example : replaced.getExamples()) {

						String leftOver = example;
						for (int i = 0; i < splitter.size(); i++) {
							Pair<String, String> split = splitter.get(i).apply(leftOver);
							components.get(i).getExamples().add(split.getLeft());
							leftOver = split.getRight();
						}
						components.get(components.size() - 1).getExamples().add(leftOver);
					}

					// create edges to new nodes
					List<Edge> newEdges = new ArrayList<>(components.size());
					for (PrimitiveNode node : components) {
						Edge e = new Edge(compositeNode.getUuid(), node.getUuid());
						newEdges.add(e);
					}

					// add new nodes and edges to model
					syntaxModel.getNodes().addAll(components);
					syntaxModel.getEdges().addAll(newEdges);
				}
			}
		}

		// update entities
		if (CollectionUtilities.hasElements(delta.getEntities())) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			semanticModel.getNodes().removeAll(delta.getEntities());
			if (!delta.isDeletion()) {
				// now add the new ones with the same uuids but new content
				semanticModel.getNodes().addAll(delta.getEntities());
			}
		}

		if (!delta.isDeletion() && CollectionUtilities.hasElements(delta.getNodes()) && delta.getNodes().stream().anyMatch(schemaNode -> schemaNode instanceof CompositeNode)) {
			// check that possible changes to the syntax model (e.g. splits) are properly reflected in the entities
			semanticModel.getNodes().stream()
					.filter(entityType -> entityType.getSyntaxNodeUuid() != null)
					.forEach(entityType -> {
						// search for the corresponding syntax node and check if it is still a primary node
						Optional<SchemaNode> match = delta.getNodes().stream().filter(schemaNode -> schemaNode.getUuid().equals(entityType.getSyntaxNodeUuid())).findFirst();
						if (match.isPresent() && !(match.get() instanceof PrimitiveNode)) {
							// not a primitive node any more, remove the link
							entityType.setSyntaxNodeUuid(null);
							entityType.setOriginalLabel(null);

							// also remove the explicit mapping
							getPrimitiveEntityTypeMappings().removeIf(mapping -> mapping.getPrimitiveUUID().equals(match.get().getUuid()));
						}
					});
		}


		// update relations
		if (CollectionUtilities.hasElements(delta.getRelations())) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			semanticModel.getEdges().removeAll(delta.getRelations());
			if (!delta.isDeletion()) {
				// now add the new ones with the same uuids but new content
				semanticModel.getEdges().addAll(delta.getRelations());
			}
		}

		// check consistency of semantic model in case elements have been removed
		if (delta.isDeletion()) {
			List<String> deletedTypeIds = delta.getEntities().stream().map(CombinedModelElement::getUuid).collect(Collectors.toList());
			// remove dangling edges
			semanticModel.getEdges().removeIf(relation -> deletedTypeIds.contains(relation.getFromId()) || deletedTypeIds.contains(relation.getToId()));

			// remove mappings that mapped deleted entities to syntax nodes
			getPrimitiveEntityTypeMappings().removeIf(mapping -> deletedTypeIds.contains(mapping.getEntityTypeId()));
		}
	}


	@Override
	public int hashCode() {
		return Objects.hash(syntaxModel, semanticModel, recommendations);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !getClass().equals(o.getClass())) {
			return false;
		}
		CombinedModel that = (CombinedModel) o;
		return Objects.equals(syntaxModel, that.syntaxModel) &&
			   Objects.equals(semanticModel, that.semanticModel);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"CombinedModel\""
				+ ", \"syntaxModel\":" + syntaxModel
				+ ", \"semanticModel\":" + semanticModel
				+ ", \"primitiveEntityTypeMappings\":" + StringUtilities.listToJson(primitiveEntityTypeMappings)
				+ ", \"recommendations\":" + StringUtilities.listToJson(recommendations)
				+ '}';
	}
}
