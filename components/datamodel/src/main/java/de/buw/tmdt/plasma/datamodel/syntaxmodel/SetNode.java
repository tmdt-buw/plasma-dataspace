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
 * Represents a set. All outgoing {@link Edge}s' target nodes represent members of that set.
 */
@JsonTypeName("SetNode")
public class SetNode extends SchemaNode {

    private static final long serialVersionUID = -5424468053886038114L;

    public SetNode(
            @Nullable List<SyntacticOperationDTO> operations,
            boolean isValid
    ) {
        super("", operations, isValid);
    }

	@JsonCreator
	public SetNode(
            @JsonProperty @NotNull String uuid,
            @JsonProperty @NotNull String label,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate,
            @JsonProperty @NotNull List<SyntacticOperationDTO> operations,
            @JsonProperty boolean isValid
    ) {
        super(uuid, label, xCoordinate, yCoordinate, operations, isValid);
    }

    @Override
    public SetNode copy() {
        return new SetNode(
                getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate(),
                CollectionUtilities.map(getOperations(),
                        SyntacticOperationDTO::copy,
                        ArrayList::new),
                isValid()
        );
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"" + this.getClass().getSimpleName() + "\""
                + ", \"@super\":" + super.toString()
                + '}';
    }
}
