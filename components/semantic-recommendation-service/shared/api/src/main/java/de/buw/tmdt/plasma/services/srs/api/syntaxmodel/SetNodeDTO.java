package de.buw.tmdt.plasma.services.srs.api.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.services.srs.api.syntaxmodel.operation.SyntacticOperationDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SetNodeDTO extends SchemaNodeDTO {

	private static final long serialVersionUID = -5424468053886038114L;

	@JsonCreator
	public SetNodeDTO(
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
			@JsonProperty(UUID_PROPERTY) @NotNull UUID uuid,
			@JsonProperty(LABEL_PROPERTY) @NotNull String label,
			@JsonProperty(OPERATIONS_PROPERTY) @NotNull List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID__PROPERTY) boolean isValid
	) {
		super(xCoordinate, yCoordinate, uuid, label, operations, isValid);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"SetNodeDTO\""
		       + ", \"@super\":" + super.toString()
		       + '}';
	}
}
