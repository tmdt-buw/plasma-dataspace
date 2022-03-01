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
import java.util.UUID;

/**
 * This node represents a node that is the root of an object.
 * All outgoing edges are to be considered child nodes are to be considered to represent attributes.
 */
@JsonTypeName("ObjectNode")
public class ObjectNode extends SchemaNode {

    private static final long serialVersionUID = 8596616883718165165L;

    public ObjectNode(
            @NotNull String label,
            @Nullable List<SyntacticOperationDTO> operations,
            boolean isValid
    ) {
        super(label, operations == null ? new ArrayList<>() : operations, isValid);
    }

    public ObjectNode(
            @NotNull String label,
            @Nullable Double xCoordinate,
            @Nullable Double yCoordinate,
            @NotNull List<SyntacticOperationDTO> operations,
            boolean isValid
    ) {
        super(UUID.randomUUID().toString(), label, xCoordinate, yCoordinate, operations, isValid);
    }

    @JsonCreator
    public ObjectNode(
            @NotNull @JsonProperty String uuid,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate,
            @JsonProperty @NotNull List<SyntacticOperationDTO> operations,
            @JsonProperty boolean isValid
    ) {
        super(uuid, label, xCoordinate, yCoordinate, operations, isValid);
    }

    @Override
    public ObjectNode copy() {
        return new ObjectNode(getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate(),
                CollectionUtilities.map(getOperations(),
                        SyntacticOperationDTO::copy,
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
