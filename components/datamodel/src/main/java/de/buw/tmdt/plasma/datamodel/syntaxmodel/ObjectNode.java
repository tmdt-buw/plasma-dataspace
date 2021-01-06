package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This node represents a node that is the root of an object.
 * All outgoing edges are to be considered child nodes are to be considered to represent attributes.
 */
@JsonTypeName("ObjectNode")
public class ObjectNode extends SchemaNode implements Cloneable {

    private static final long serialVersionUID = 8596616883718165165L;

    public ObjectNode(
            @NotNull String label,
            @Nullable List<SyntacticOperationDTO> operations,
            boolean isValid
    ) {
        super(label, operations == null ? new ArrayList<>() : operations, isValid);
    }

	@JsonCreator
	public ObjectNode(
            @NotNull @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
            @Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
            @JsonProperty(OPERATIONS_PROPERTY) @NotNull List<SyntacticOperationDTO> operations,
            @JsonProperty(IS_VALID_PROPERTY) boolean isValid
    ) {
        super(uuid, label, xCoordinate, yCoordinate, operations, isValid);
    }

    @Override
    public ObjectNode clone() {
        return new ObjectNode(getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate(),
                CollectionUtilities.map(getOperations(),
                        SyntacticOperationDTO::clone,
                        ArrayList::new),
                isValid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"ObjectNodeDTO\""
                + ", \"@super\":" + super.toString()
                + '}';
    }
}
