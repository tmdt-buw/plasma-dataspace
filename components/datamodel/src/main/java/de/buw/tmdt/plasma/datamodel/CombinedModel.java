package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
	private final ArrayList<PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs;
	/**
	 * Recommendations represent suggested modifications for the current state of models
	 */
	private ArrayList<DeltaModification> recommendations;
	/**
	 * A finalized model should not be changed any more
	 */
	private final boolean finalized;

	public CombinedModel(
			@JsonProperty(SYNTAX_MODEL_PROPERTY) @NotNull SyntaxModel syntaxModel,
			@JsonProperty(SEMANTIC_MODEL_PROPERTY) SemanticModel semanticModel,
			@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST) List<? extends PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs,
			@JsonProperty(RECOMMENDATION_LIST) List<DeltaModification> recommendations,
			@JsonProperty(value = FINALIZED, defaultValue = "false") boolean finalized
	) {
		this.syntaxModel = syntaxModel;
		this.semanticModel = semanticModel == null ? new SemanticModel(null,null) : semanticModel;
		this.primitiveEntityTypeEdgeDTOs = primitiveEntityTypeEdgeDTOs == null ? new ArrayList<>() : new ArrayList<>(primitiveEntityTypeEdgeDTOs);
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
	public List<PrimitiveEntityTypeEdgeDTO> getPrimitiveEntityTypeEdgeDTOs() {
		return primitiveEntityTypeEdgeDTOs;
	}

	@NotNull
	@JsonProperty(RECOMMENDATION_LIST)
	public ArrayList<DeltaModification> getRecommendations() {
		return recommendations;
	}

	// we allow later injections of recommendations, will be solved with new data model
	public void setRecommendations(ArrayList<DeltaModification> recommendations) {
		this.recommendations = recommendations;
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
		       + ", \"primitiveEntityTypeEdgeDTOs\":" + StringUtilities.listToJson(primitiveEntityTypeEdgeDTOs)
				+ ", \"recommendations\":" + StringUtilities.listToJson(recommendations)
		       + '}';
	}
}
