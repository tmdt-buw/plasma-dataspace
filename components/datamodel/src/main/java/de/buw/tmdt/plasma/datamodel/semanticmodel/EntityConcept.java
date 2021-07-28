package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class EntityConcept extends CombinedModelElement implements Serializable {
    private static final long serialVersionUID = 2351939502355153654L;

    public static final String ENTITY_ID_PROPERTY = "entityId";
    private static final String DESCRIPTION_PROPERTY = "description";
    private static final String SOURCE_URI_PROPERTY = "sourceURI";

    /**
     * Entity ID, set once sent to the KGS.
     */
    private String entityId;
    private final String description;
    @NotNull
    private final String sourceURI;

    public EntityConcept(
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @NotNull @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI
    ) {
        super(label);
        this.entityId = null;
        this.description = description;
        this.sourceURI = sourceURI;
    }

    @JsonCreator
    public EntityConcept(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @NotNull @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI
    ) {
        super(uuid, label);
        this.entityId = entityId;
        this.description = description;
        this.sourceURI = sourceURI;
    }


    @JsonProperty(ENTITY_ID_PROPERTY)
    @Nullable
    public String getEntityId() {
        return entityId;
    }

    @JsonProperty(SOURCE_URI_PROPERTY)
    @NotNull
    public String getSourceURI() {
        return sourceURI;
    }

    @JsonProperty(DESCRIPTION_PROPERTY)
    public String getDescription() {
        return description;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * Clones this {@link EntityConcept}.
     *
     * @return the cloned concept
     */
    @Override
    public EntityConcept copy() {
        return new EntityConcept(getUuid(), getLabel(), getEntityId(), getDescription(), getSourceURI());
    }

    public int hashCode() {
        return getUuid().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            EntityConcept that = (EntityConcept) o;
            return Objects.equals(this.getUuid(), that.getUuid());
        } else {
            return false;
        }
    }

    /**
     * Identifies if two {@link EntityConcept}s represent the same concept.
     * Needed as there might exist multiple {@link EntityConcept} instances that actually represent the same concept
     * but have different uuids.
     *
     * @param ec The {@link EntityConcept} to check
     * @return true of the {@link #sourceURI}s of both instances match
     */
    public boolean sameAs(EntityConcept ec) {
        return Objects.equals(getSourceURI(), ec.getSourceURI());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"EntityConcept\""
                + ", \"uuid\":\"" + getUuid() + '"'
                + ", \"entityId\":\"" + entityId + '"'
                + ", \"label\":\"" + getLabel() + '"'
                + ", \"sourceURI\":\"" + sourceURI + '"'
                + ", \"description\":\"" + description + '"'
                + '}';
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getLabel().trim().length() < 1) {
            throw new CombinedModelIntegrityException("Label of EntityConcept '" + getLabel() + "' must not be shorter than 1 characters.");
        }
        if (getSourceURI().trim().length() < 3) {
            throw new CombinedModelIntegrityException("SourceURI of EntityConcept '" + getLabel() + "' must not be shorter than 3 characters.");
        }
    }
}