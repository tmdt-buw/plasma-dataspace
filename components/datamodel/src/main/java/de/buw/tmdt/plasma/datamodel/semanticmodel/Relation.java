package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
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
public abstract class Relation extends CombinedModelElement implements Serializable, RelationTemplate {
    private static final long serialVersionUID = -2964347710102077197L;

    /**
     * The uuid of the origin element, identified by CombinedModelElement#uuid.
     */
    private String fromId;

    /**
     * The uuid of the destination element, identified by CombinedModelElement#uuid.
     */
    private String toId;


    @NotNull
    private final String uri;
    private String description;
    private Set<String> properties;

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String uri
    ) {
        this(null, fromId, toId, uri, null, null, null);
    }

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String uri,
            @NotNull String label
    ) {
        this(null, fromId, toId, uri, label, null, null);
    }

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull String uri,
            @NotNull String label,
            @NotNull String description
    ) {
        this(null, fromId, toId, uri, label, description, null);
    }

    @JsonCreator
    public Relation(
            @Nullable @JsonProperty String uuid,
            @NotNull @JsonProperty String fromId,
            @NotNull @JsonProperty String toId,
            @NotNull @JsonProperty String uri,
            @Nullable @JsonProperty String label,
            @Nullable @JsonProperty String description,
            @Nullable @JsonProperty Set<String> properties
    ) {
        super(uuid, label == null ? "" : label);
        this.fromId = fromId;
        this.toId = toId;
        this.description = description;
        this.uri = uri;
        this.properties = properties == null ? new HashSet<>() : new HashSet<>(properties);
    }

    @JsonProperty
    public String getFromId() {
        return fromId;
    }

    public void setFromId(@NotNull String fromId) {
        this.fromId = fromId;
    }

    @JsonProperty
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
    public String getURI() {
        return uri;
    }

    @JsonProperty
    public Set<String> getProperties() {
        return Collections.unmodifiableSet(this.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, uri);
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
        return Objects.equals(this.getURI(), that.getURI()) &&
                // we cannot verify this at the moment
                //Objects.equals(this.getFromId(), that.getFromId()) &&
                //Objects.equals(this.getToId(), that.getToId()) &&
                Objects.equals(this.getLabel(), that.getLabel());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"Relation\""
                + ", \"fromId\":\"" + fromId + '"'
                + ", \"toId\":\"" + toId + '"'
                + ", \"URI\":" + uri
                + '}';
    }

    public abstract Relation copy();

    public abstract Relation instanciate(String fromId, String toId);

    /**
     * Removes all ids from this relation.
     * Used ONLY when used as a template, e.g. in serialization to frontend.
     * For usage within a Java environment, use {@link #instanciate(String, String)} to create a new entity with set ids
     */
    @SuppressWarnings("ConstantConditions") // this is only to be invoked before serialization
    public Relation convertToTemplate() {
        this.fromId = null;
        this.toId = null;
        return this;
    }

    @Override
    public boolean isTemplate() {
        return getUuid() == null;
    }

    public void validate() {
        if (getFromId() == null || getFromId().isBlank()) {
            throw new CombinedModelIntegrityException("Relation " + getUuid() + " has no proper fromId");
        }
        if (getToId() == null || getToId().isBlank()) {
            throw new CombinedModelIntegrityException("Relation " + getUuid() + " has no proper toId");
        }
        if (getURI() == null || getURI().isBlank()) {
            throw new CombinedModelIntegrityException("Relation " + getUuid() + " has no proper URL assigned");
        }
    }

    /**
     * Identifies if two {@link Relation}s represent the same entity.
     * Needed as there might exist multiple {@link Relation}s instances that actually represent the same relation
     * or class but have different uuids.
     *
     * @param relation The {@link Relation} to check
     * @return true of the {@code uri}s of both instances match
     */
    public boolean sameAs(Relation relation) {
        return Objects.equals(getURI(), relation.getURI());
    }


}
