package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
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
     * Entity ID, set once sent to the KGS
     */
    private final String entityId;
    private final String description;
    private final String sourceURI;

    public EntityConcept(
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @Nullable @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI
    ) {
        super(label);
        this.entityId = null;
        this.description = description;
        this.sourceURI = sourceURI;
    }

    @JsonCreator
    public EntityConcept(
            @NotNull @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @Nullable @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI
    ) {
        super(uuid,label);
        this.entityId = entityId;
        this.description = description;
        this.sourceURI = sourceURI;
    }


    @JsonProperty(ENTITY_ID_PROPERTY)
    @Nullable
    public String getEntityId() {
        return StringUtilities.toStringIfExists(entityId);
    }

    @JsonProperty(SOURCE_URI_PROPERTY)
    @NotNull
    public String getSourceURI() {
        return sourceURI;
    }

    @JsonProperty(DESCRIPTION_PROPERTY)
    @NotNull
    public String getDescription() {
        return description;
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

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"EntityConceptDTO\""
                + ", \"uuid\":\"" + getUuid() + '"'
               + ", \"entityId\":\"" + entityId + '"'
               + ", \"label\":\"" + getLabel()+ '"'
               + ", \"sourceURI\":\"" + sourceURI + '"'
               + ", \"description\":\"" + description + '"'
               + '}';
    }
}