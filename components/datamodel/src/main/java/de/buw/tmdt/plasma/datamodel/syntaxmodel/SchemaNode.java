package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.*;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
		@JsonSubTypes.Type(value = PrimitiveNode.class, name = "PrimitiveNode"),
		@JsonSubTypes.Type(value = ObjectNode.class, name = "ObjectNode"),
		@JsonSubTypes.Type(value = SetNode.class, name = "SetNode"),
		@JsonSubTypes.Type(value = CollisionSchema.class, name = "CollisionSchema"),
		@JsonSubTypes.Type(value = CompositeNode.class, name = "Composite")
})
public abstract class SchemaNode extends PositionedCombinedModelElement {

	protected static final String IS_VALID_PROPERTY = "isValid";
	protected static final String OPERATIONS_PROPERTY = "operations";
	private static final long serialVersionUID = 8398288916936841021L;

	private final boolean isValid;
	/**
	 * Holds a set of possible specific operations that are possible on this element.
	 */
	private final ArrayList<SyntacticOperationDTO> operations;

	public SchemaNode(
			 String label,
			 List<SyntacticOperationDTO> operations,
			 boolean isValid
	) {
		super(label);
		this.isValid = isValid;
		if (CollectionUtilities.containsNull(operations)) {
			throw new IllegalArgumentException("Operations must not contain null.");
		}
		this.operations = new ArrayList<>(operations);
	}

	@JsonCreator
	public SchemaNode(
			@NotNull @JsonProperty(UUID_PROPERTY)  String uuid,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
			@NotNull @JsonProperty(OPERATIONS_PROPERTY)  List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid
	) {
		super(uuid,label,xCoordinate, yCoordinate);
		this.isValid = isValid;
		if (CollectionUtilities.containsNull(operations)) {
			throw new IllegalArgumentException("Operations must not contain null.");
		}
		this.operations = new ArrayList<>(operations);
	}

	@JsonProperty(IS_VALID_PROPERTY)
	public boolean isValid() {
		return isValid;
	}

	@JsonProperty(OPERATIONS_PROPERTY)
	@NotNull
	public List<SyntacticOperationDTO> getOperations() {
		return operations;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUuid(), isValid, operations);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !getClass().equals(o.getClass())) {
			return false;
		}
		SchemaNode that = (SchemaNode) o;
		return isValid == that.isValid &&
		       Objects.equals(getUuid(), getUuid()) &&
		       Objects.equals(operations, that.operations);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"SchemaNodeDTO\""
		       + ", \"@super\":" + super.toString()
		       + ", \"uuid\":" + getUuid()
		       + ", \"isValid\":\"" + isValid + '"'
		       + ", \"operations\":" + StringUtilities.listToJson(operations)
		       + '}';
	}
}
