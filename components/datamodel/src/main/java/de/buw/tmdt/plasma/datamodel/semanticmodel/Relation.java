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
    private String fromId;

    /**
     * The uuid of the destination element, identified by CombinedModelElement#uuid.
     */
    private String toId;
    private RelationConcept concept;

    public Relation(
            String fromId,
            String toId,
            RelationConcept concept
    ) {
        this(null, fromId, toId, concept);
    }

    public Relation(
            String entityId,
            String fromId,
            String toId,
            RelationConcept concept
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
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonProperty(FROM_ID_PROPERTY)
    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    @JsonProperty(TO_ID_PROPERTY)
    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    @JsonProperty(CONCEPT_PROPERTY)
    public RelationConcept getConcept() {
        return concept;
    }

    public void setConcept(RelationConcept concept) {
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
