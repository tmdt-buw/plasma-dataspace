package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Instance.class, name = "Instance"),
        @JsonSubTypes.Type(value = Literal.class, name = "Literal"),
        @JsonSubTypes.Type(value = Class.class, name = "SemanticClass"),
        @JsonSubTypes.Type(value = NamedEntity.class, name = "NamedEntity")
})
public abstract class SemanticModelNode extends PositionedCombinedModelElement {

    private final String uri;

    public SemanticModelNode(@Nullable String uri, @NotNull String label) {
        this(null, uri, label, null, null);
    }

    public SemanticModelNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        this(null, uri, label, xCoordinate, yCoordinate);
    }

    public SemanticModelNode(@Nullable String uuid, @Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        super(uuid, label, xCoordinate, yCoordinate);
        this.uri = uri;
    }

    /**
     * Identifies if two {@link SemanticModelNode}s represent the same entity.
     * Needed as there might exist multiple {@link SemanticModelNode} instances that actually represent the same concept
     * or class but have different uuids.
     *
     * @param ec The {@link SemanticModelNode} to check
     * @return true of the {@code uri}s of both instances match
     */
    public boolean sameAs(SemanticModelNode ec) {
        return Objects.equals(getURI(), ec.getURI());
    }

    @JsonProperty
    public String getURI() {
        return uri;
    }

    public abstract SemanticModelNode copy();

    /**
     * Utility function to generate a new {@link SemanticModelNode} (or one of its subtypes) from another node.
     *
     * @return A new instance of the same type, with all values except 'uuid' copied. A new 'uuid' is generated for the new element.
     */
    public abstract SemanticModelNode build();

    public abstract void validate() throws CombinedModelIntegrityException;
}
