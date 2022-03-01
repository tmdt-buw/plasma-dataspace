package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class Class extends SemanticModelNode implements Serializable {
    private static final long serialVersionUID = 2351939502355153654L;

    private String description;

    /**
     * The customized {@link Instance} for this class.
     * Can be null if no modifications have been made.
     */
    @Nullable
    private Instance instance;

    public Class(
            @NotNull @JsonProperty String uri,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty String description
    ) {
        this(null, uri, label, description, null, null, null);
    }

    public Class(String uuid, String uri, String label, String description, Double xCoordinate, Double yCoordinate) {
        this(uuid, uri, label, description, xCoordinate, yCoordinate, null);
    }

    @JsonCreator
    public Class(
            @Nullable @JsonProperty String uuid,
            @NotNull @JsonProperty String uri,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty String description,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate,
            @Nullable @JsonProperty Instance instance
    ) {
        super(uuid, uri, label, xCoordinate, yCoordinate);
        this.description = description;
        this.instance = instance;
    }


    @JsonProperty
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty
    public Instance getInstance() {
        return instance;
    }

    public void setInstance(@Nullable Instance instance) {
        this.instance = instance;
    }

    /**
     * Clones this {@link Class}.
     *
     * @return the cloned concept
     */
    @Override
    public Class copy() {
        return new Class(getUuid(), getURI(), getLabel(), getDescription(), getXCoordinate(), getYCoordinate(), getInstance());
    }

    @Override
    public SemanticModelNode instanciate() {
        Class newClass = copy();
        newClass.setInstance(null);
        newClass.setXCoordinate(null);
        newClass.setYCoordinate(null);
        return newClass;
    }

    public int hashCode() {
        return getUuid().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            Class that = (Class) o;
            return Objects.equals(this.getURI(), that.getURI()) &&
                    Objects.equals(this.getLabel(), that.getLabel()) &&
                    Objects.equals(this.getDescription(), that.getDescription()) &&
                    Objects.equals(this.getXCoordinate(), that.getXCoordinate()) &&
                    Objects.equals(this.getYCoordinate(), that.getYCoordinate()) &&
                    Objects.equals(this.getInstance(), that.getInstance());
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "Class{" +
                "uri='" + getURI() + '\'' +
                ", label='" + getLabel() + '\'' +
                ", description='" + description + '\'' +
                ", instance=" + instance +
                '}';
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getLabel().trim().length() < 1) {
            throw new CombinedModelIntegrityException("Label of EntityConcept '" + getLabel() + "' must not be shorter than 1 characters.");
        }
        if (getURI().trim().length() < 3) {
            throw new CombinedModelIntegrityException("SourceURI of EntityConcept '" + getLabel() + "' must not be shorter than 3 characters.");
        }
        if (instance != null) {
            instance.validate();
        }
    }

    /**
     * Creates a default {@link Instance} for this {@link Class}.
     * The default instance uses the label from this {@link Class}.
     * Will do nothing if an {@link Instance} is already defined.
     */
    public void createInstance() {
        instance = new Instance(getLabel(), "");
    }
}