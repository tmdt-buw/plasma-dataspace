package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class SemanticClass extends CombinedModelElement implements Serializable, URIDefinedElement {
    private static final long serialVersionUID = 2351939502355153654L;
    private final String description;
    @NotNull
    private final String uri;
    /**
     * Entity ID, set once sent to the KGS.
     */
    private String entityId;

    public SemanticClass(
            @NotNull @JsonProperty String uri,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty String description
    ) {
        this(null, uri, label, description);
    }

    @JsonCreator
    public SemanticClass(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty String uri,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty String description
    ) {
        super(uuid, label);
        this.description = description;
        this.uri = uri;
    }


    @JsonProperty
    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonProperty
    @NotNull
    public String getURI() {
        return uri;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    /**
     * Clones this {@link SemanticClass}.
     *
     * @return the cloned concept
     */
    @Override
    public SemanticClass copy() {
        return new SemanticClass(getUuid(), getURI(), getLabel(), getDescription());
    }

    public int hashCode() {
        return getUuid().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            SemanticClass that = (SemanticClass) o;
            return Objects.equals(this.getUuid(), that.getUuid());
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "SemanticClass{" +
                "entityId='" + entityId + '\'' +
                ", description='" + description + '\'' +
                ", sourceURI='" + uri + '\'' +
                '}';
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getLabel().trim().length() < 1) {
            throw new CombinedModelIntegrityException("Label of EntityConcept '" + getLabel() + "' must not be shorter than 1 characters.");
        }
        if (getURI().trim().length() < 3) {
            throw new CombinedModelIntegrityException("SourceURI of EntityConcept '" + getLabel() + "' must not be shorter than 3 characters.");
        }
    }
}