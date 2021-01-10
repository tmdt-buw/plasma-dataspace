package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class Relation extends CombinedModelElement implements Serializable {
    public static final String ENTITY_ID_PROPERTY = "entityId";
    public static final String FROM_ID_PROPERTY = "from";
    public static final String TO_ID_PROPERTY = "to";
    public static final String CONCEPT_PROPERTY = "concept";
    private static final long serialVersionUID = -2964347710102077197L;
    /**
     * Knowledge base ID, set once sent to the KGS.
     */
    private String entityId;

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
    private RelationConcept concept;

    public Relation(
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull RelationConcept concept
    ) {
        this(null, fromId, toId, concept);
    }

    public Relation(
            @Nullable String entityId,
            @NotNull String fromId,
            @NotNull String toId,
            @NotNull RelationConcept concept
    ) {
        super("");
        this.entityId = entityId;
        this.fromId = fromId;
        this.toId = toId;
        this.concept = concept;
    }

    @JsonCreator
    public Relation(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
            @NotNull @JsonProperty(FROM_ID_PROPERTY) String fromId,
            @NotNull @JsonProperty(TO_ID_PROPERTY) String toId,
            @NotNull @JsonProperty(CONCEPT_PROPERTY) RelationConcept concept
    ) {
        super(uuid, "");
        this.entityId = entityId;
        this.fromId = fromId;
        this.toId = toId;
        this.concept = concept;
    }

    @JsonProperty(ENTITY_ID_PROPERTY)
    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(@NotNull String entityId) {
        this.entityId = entityId;
    }

    @JsonProperty(FROM_ID_PROPERTY)
    @NotNull
    public String getFromId() {
        return fromId;
    }

    public void setFromId(@NotNull String fromId) {
        this.fromId = fromId;
    }

    @JsonProperty(TO_ID_PROPERTY)
    @NotNull
    public String getToId() {
        return toId;
    }

    public void setToId(@NotNull String toId) {
        this.toId = toId;
    }

    @JsonProperty(CONCEPT_PROPERTY)
    @NotNull
    public RelationConcept getConcept() {
        return concept;
    }

    public void setConcept(@NotNull RelationConcept concept) {
        this.concept = concept;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity with also the RelationConcept cloned
     */
    public Relation copy() {
        return new Relation(getUuid(),
                getEntityId(),
                getFromId(),
                getToId(),
                getConcept().copy()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, concept);
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
        return "{\"@class\":\"Relation\""
                + ", \"fromId\":\"" + fromId + '"'
                + ", \"toId\":\"" + toId + '"'
                + ", \"concept\":" + concept
                + '}';
    }
}
