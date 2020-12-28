package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

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
			@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST) List<? extends PrimitiveEntityTypeMapping> primitiveEntityTypeEdgeDTOs,
			@JsonProperty(RECOMMENDATION_LIST) List<DeltaModification> recommendations,
			@JsonProperty(value = FINALIZED, defaultValue = "false") boolean finalized
	) {
		this.syntaxModel = syntaxModel;
		this.semanticModel = semanticModel == null ? new SemanticModel(null,null) : semanticModel;
		this.primitiveEntityTypeMappings = primitiveEntityTypeEdgeDTOs == null ? new ArrayList<>() : new ArrayList<>(primitiveEntityTypeEdgeDTOs);
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
		if(finalized){
			throw new UnsupportedOperationException("Model is finalized. No more modifications possible");
		}
		// apply syntactic operations
// update nodes
		if (delta.getNodes() != null) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all nodes that are to be overwritten (i.e. uuids match)
			syntaxModel.getNodes().removeAll(delta.getNodes());
			// now add the new ones with the same uuids but new content
			syntaxModel.getNodes().addAll(delta.getNodes());
		}
		// update edges
		if (delta.getEdges() != null) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			syntaxModel.getEdges().removeAll(delta.getEdges());
			// now add the new ones with the same uuids but new content
			syntaxModel.getEdges().addAll(delta.getEdges());
		}

		// update entities
		if (delta.getEntities() != null) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			semanticModel.getNodes().removeAll(delta.getEntities());
			// now add the new ones with the same uuids but new content
			semanticModel.getNodes().addAll(delta.getEntities());
		}
		// update relations
		if (delta.getRelations() != null) {
			// as we match by uuid only, we can use removeAll and addAll
			// remove all edges that are to be overwritten (i.e. uuids match)
			semanticModel.getEdges().removeAll(delta.getRelations());
			// now add the new ones with the same uuids but new content
			semanticModel.getEdges().addAll(delta.getRelations());
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
		return "{\"@class\":\"DataSourceSchemaDTO\""
		       + ", \"syntaxModelDTO\":" + syntaxModel
		       + ", \"semanticModelDTO\":" + semanticModel
		       + ", \"primitiveEntityTypeEdgeDTOs\":" + StringUtilities.listToJson(primitiveEntityTypeMappings)
				+ ", \"recommendations\":" + StringUtilities.listToJson(recommendations)
		       + '}';
	}
}
