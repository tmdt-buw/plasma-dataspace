package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PrimitiveNode.class, name = "PrimitiveNode"),
        @JsonSubTypes.Type(value = ObjectNode.class, name = "ObjectNode"),
        @JsonSubTypes.Type(value = SetNode.class, name = "SetNode"),
        @JsonSubTypes.Type(value = CollisionSchema.class, name = "CollisionSchema"),
        @JsonSubTypes.Type(value = CompositeNode.class, name = "CompositeNode")
})
public abstract class SchemaNode extends PositionedCombinedModelElement {

    private static final long serialVersionUID = 8398288916936841021L;

    public static final String OPERATIONS_PROPERTY = "operations";
    public static final String VALID_PROPERTY = "valid";

    private final boolean isValid;
    /**
     * Holds a set of possible specific operations that are possible on this element.
     */
    private final ArrayList<SyntacticOperationDTO> operations;

    public SchemaNode(
            @NotNull String label,
            boolean isValid
    ) {
        super(label);
        this.isValid = isValid;
        this.operations = new ArrayList<>();
    }

    @JsonCreator
    public SchemaNode(
            @NotNull @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(XCOORDINATE_PROPERTY) Double xCoordinate,
            @Nullable @JsonProperty(YCOORDINATE_PROPERTY) Double yCoordinate,
            @JsonProperty(VALID_PROPERTY) boolean isValid
    ) {
        super(uuid, label, xCoordinate, yCoordinate);
        this.isValid = isValid;
        this.operations = new ArrayList<>();
    }

    @JsonProperty(VALID_PROPERTY)
    public boolean isValid() {
        return isValid;
    }

    @JsonProperty(value = OPERATIONS_PROPERTY, access = JsonProperty.Access.READ_ONLY)
    public List<SyntacticOperationDTO> getOperations() {
        return operations;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    public abstract SchemaNode copy();

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), isValid, operations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaNode)) {
            return false;
        }
        SchemaNode that = (SchemaNode) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"SchemaNode\""
                + ", \"@super\":" + super.toString()
                + ", \"uuid\":" + getUuid()
                + ", \"isValid\":\"" + isValid + '"'
                + ", \"operations\":" + StringUtilities.listToJson(operations)
                + '}';
    }
}
