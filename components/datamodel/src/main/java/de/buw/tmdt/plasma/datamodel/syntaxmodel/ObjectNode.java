package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonTypeName("ObjectNode")
public class ObjectNode extends SchemaNode {

	private static final long serialVersionUID = 8596616883718165165L;

	public ObjectNode(
			@NotNull  String label,
			 @NotNull List<SyntacticOperationDTO> operations,
			 boolean isValid
	) {
		super( label,operations,  isValid);
	}

	@JsonCreator
	public ObjectNode(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
			@JsonProperty(OPERATIONS_PROPERTY) @NotNull List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid
	) {
		super( uuid, databaseId,label,xCoordinate, yCoordinate, operations, isValid);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"ObjectNodeDTO\""
		       + ", \"@super\":" + super.toString()
		       + '}';
	}
}
