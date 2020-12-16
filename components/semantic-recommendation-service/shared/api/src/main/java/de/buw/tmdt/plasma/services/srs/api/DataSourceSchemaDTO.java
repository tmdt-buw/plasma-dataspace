package de.buw.tmdt.plasma.services.srs.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.services.srs.api.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.srs.api.syntaxmodel.SyntaxModelDTO;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DataSourceSchemaDTO implements Serializable {

	public static final String SYNTAX_MODEL_PROPERTY = "syntaxModel";
	public static final String SEMANTIC_MODEL_PROPERTY = "semanticModel";
	private static final String PRIMITIVE_ENTITY_TYPE_LIST = "primitiveToEntityTypeList";
	private static final String FINALIZED = "finalized";
	private static final long serialVersionUID = 1069167546126071914L;

	private final SyntaxModelDTO syntaxModelDTO;
	private final SemanticModelDTO semanticModelDTO;
	private final ArrayList<PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs;
	private final boolean finalized;

	public DataSourceSchemaDTO(
			@JsonProperty(SYNTAX_MODEL_PROPERTY) @NotNull SyntaxModelDTO syntaxModelDTO,
			@JsonProperty(SEMANTIC_MODEL_PROPERTY) SemanticModelDTO semanticModelDTO,
			@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST) List<? extends PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs,
			@JsonProperty(value = FINALIZED, defaultValue = "false") boolean finalized
	) {
		this.syntaxModelDTO = syntaxModelDTO;
		this.semanticModelDTO = semanticModelDTO;
		this.primitiveEntityTypeEdgeDTOs = new ArrayList<>(primitiveEntityTypeEdgeDTOs);
		this.finalized = finalized;
	}

	@JsonProperty(SYNTAX_MODEL_PROPERTY)
	public SyntaxModelDTO getSyntaxModelDTO() {
		return syntaxModelDTO;
	}

	@JsonProperty(SEMANTIC_MODEL_PROPERTY)
	public SemanticModelDTO getSemanticModelDTO() {
		return semanticModelDTO;
	}

	@JsonProperty(value = FINALIZED, defaultValue = "false")
	public boolean isFinalized() {
		return finalized;
	}

	@JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST)
	public List<PrimitiveEntityTypeEdgeDTO> getPrimitiveEntityTypeEdgeDTOs() {
		return Collections.unmodifiableList(primitiveEntityTypeEdgeDTOs);
	}

	@Override
	public int hashCode() {

		return Objects.hash(syntaxModelDTO, semanticModelDTO);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !getClass().equals(o.getClass())) {
			return false;
		}
		DataSourceSchemaDTO that = (DataSourceSchemaDTO) o;
		return Objects.equals(syntaxModelDTO, that.syntaxModelDTO) &&
		       Objects.equals(semanticModelDTO, that.semanticModelDTO);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"DataSourceSchemaDTO\""
		       + ", \"syntaxModelDTO\":" + syntaxModelDTO
		       + ", \"semanticModelDTO\":" + semanticModelDTO
		       + ", \"primitiveEntityTypeEdgeDTOs\":" + StringUtilities.listToJson(primitiveEntityTypeEdgeDTOs)
		       + '}';
	}
}
