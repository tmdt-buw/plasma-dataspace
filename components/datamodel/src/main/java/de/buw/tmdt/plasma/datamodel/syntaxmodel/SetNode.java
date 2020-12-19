package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonTypeName("SetNode")
public class SetNode extends SchemaNode {

	private static final long serialVersionUID = -5424468053886038114L;

	public SetNode(
			@JsonProperty(LABEL_PROPERTY) @NotNull String label,
			@JsonProperty(OPERATIONS_PROPERTY) @NotNull List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid
	) {
		super( label, operations,isValid);
	}

	@JsonCreator
	public SetNode(
			@JsonProperty(UUID_PROPERTY) @NotNull String uuid,
			@Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
			@JsonProperty(LABEL_PROPERTY) @NotNull String label,
			@Nullable @JsonProperty(PositionedCombinedModelElement.X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(PositionedCombinedModelElement.Y_COORDINATE_PROPERTY) Double yCoordinate,
			@JsonProperty(OPERATIONS_PROPERTY) @NotNull List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid
	) {
		super( uuid,databaseId, label,xCoordinate, yCoordinate, operations, isValid);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"SetNodeDTO\""
		       + ", \"@super\":" + super.toString()
		       + '}';
	}
}
