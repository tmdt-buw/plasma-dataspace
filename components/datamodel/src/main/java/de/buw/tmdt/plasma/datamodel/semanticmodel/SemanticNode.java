package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Literal.class, name = "Literal"),
        @JsonSubTypes.Type(value = Class.class, name = "Class"),
        @JsonSubTypes.Type(value = NamedEntity.class, name = "NamedEntity")
})
public abstract class SemanticNode extends PositionedCombinedModelElement implements NodeTemplate {

    public static final String URI_PROPERTY = "uri";
    public static final String PROVISIONAL_PROPERTY = "provisional";

    private String uri;

    private boolean provisional = false;

    public SemanticNode(@Nullable String uri, @NotNull String label) {
        this(uri, label, null, null, null);
    }

    public SemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        this(uri, label, xCoordinate, yCoordinate, null);
    }

    public SemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate, @Nullable String uuid) {
        super(uuid, label, xCoordinate, yCoordinate);
        this.uri = uri;
    }

    /**
     * Identifies if two {@link SemanticNode}s represent the same entity.
     * Needed as there might exist multiple {@link SemanticNode} instances that actually represent the same concept
     * or class but have different uuids.
     *
     * @param ec The {@link SemanticNode} to check
     * @return true of the {@code uri}s of both instances match
     */
    public boolean sameAs(SemanticNode ec) {
        return Objects.equals(getURI(), ec.getURI());
    }

    @JsonProperty(URI_PROPERTY)
    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

    @JsonProperty(PROVISIONAL_PROPERTY)
    public boolean isProvisional() {
        return provisional;
    }

    public void setProvisional(boolean provisional) {
        this.provisional = provisional;
    }

    public abstract SemanticNode copy();

    public abstract SemanticNode instanciate();

    public abstract void validate() throws CombinedModelIntegrityException;

    @Override
    public SemanticNode convertToTemplate() {
        removeUuid();
        return this;
    }

    @Override
    public boolean isTemplate() {
        return getUuid() == null;
    }

    public abstract boolean equals(Object smn);

    public boolean isMapped() {
        return false;
    }

    public boolean isLinked(){
        return false;
    }
}
