package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ObjectProperty.class, name = "ObjectProperty"),
        @JsonSubTypes.Type(value = DataProperty.class, name = "DataProperty")
})
public abstract class Relation extends CombinedModelElement implements Serializable {
    private static final long serialVersionUID = -2964347710102077197L;

    /**
     * The uuid of the origin element, identified by CombinedModelElement#uuid.
     */
    @NotNull
    private String fromId;

    /**
     * The uuid of the destination element, identified by CombinedModelElement#uuid.
     */
    @NotNull
    private String toId;


    @NotNull
    private final String sourceURI;
    private String description;
    private Set<String> properties;

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String sourceURI
    ) {
        this(null, fromId, toId, sourceURI, null, null, null);
    }

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String sourceURI,
            @NotNull String label
    ) {
        this(null, fromId, toId, sourceURI, label, null, null);
    }

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String sourceURI,
            @NotNull String label,
            @NotNull String description
    ) {
        this(null, fromId, toId, sourceURI, label, description, null);
    }

    @JsonCreator
    public Relation(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty String fromId,
            @NotNull @JsonProperty String toId,
            @NotNull @JsonProperty String sourceURI,
            @Nullable @JsonProperty String label,
            @Nullable @JsonProperty String description,
            @Nullable @JsonProperty Set<String> properties
    ) {
        super(uuid, label == null ? "" : label);
        this.fromId = fromId;
        this.toId = toId;
        this.description = description;
        this.sourceURI = sourceURI;
        this.properties = properties == null ? new HashSet<>() : new HashSet<>(properties);
    }

    @JsonProperty
    @NotNull
    public String getFromId() {
        return fromId;
    }

    public void setFromId(@NotNull String fromId) {
        this.fromId = fromId;
    }

    @JsonProperty
    @NotNull
    public String getToId() {
        return toId;
    }

    public void setToId(@NotNull String toId) {
        this.toId = toId;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public String getSourceURI() {
        return sourceURI;
    }

    @JsonProperty
    public Set<String> getProperties() {
        return Collections.unmodifiableSet(this.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, sourceURI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Relation that = (Relation) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"ObjectProperty\""
                + ", \"fromId\":\"" + fromId + '"'
                + ", \"toId\":\"" + toId + '"'
                + ", \"sourceURI\":" + sourceURI
                + '}';
    }

    public abstract Relation copy();
}
