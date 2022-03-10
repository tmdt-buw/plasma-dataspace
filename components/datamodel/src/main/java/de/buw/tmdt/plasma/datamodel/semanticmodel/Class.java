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

    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String INSTANCE_PROPERTY = "instance";

    private String description;

    /**
     * The customized {@link Instance} for this class.
     * Can be null if no modifications have been made.
     */
    @Nullable
    private Instance instance;

    public Class(
            @NotNull String uri,
            @NotNull String label,
            @Nullable String description
    ) {
        this(null, uri, label, description, null, null, null);
    }

    public Class(String uuid, String uri, String label, String description, Double xCoordinate, Double yCoordinate) {
        this(uuid, uri, label, description, xCoordinate, yCoordinate, null);
    }

    @JsonCreator
    public Class(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(URI_PROPERTY) String uri,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @Nullable @JsonProperty(XCOORDINATE_PROPERTY) Double xCoordinate,
            @Nullable @JsonProperty(YCOORDINATE_PROPERTY) Double yCoordinate,
            @Nullable @JsonProperty(INSTANCE_PROPERTY) Instance instance
    ) {
        super(uuid, uri, label, xCoordinate, yCoordinate);
        this.description = description;
        this.instance = instance;
    }


    @JsonProperty(DESCRIPTION_PROPERTY)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty(INSTANCE_PROPERTY)
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
        return new Class(getUuid(),
                getURI(),
                getLabel(),
                getDescription(),
                getXCoordinate(),
                getYCoordinate(),
                getInstance());
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

    @Override
    public boolean sameAs(SemanticModelNode o) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            Class that = (Class) o;
            return Objects.equals(this.getUuid(), that.getUuid()) &&
                    sameAs(that);
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
            throw new CombinedModelIntegrityException("Label of Class '" + getLabel() + "' must not be shorter than 1 characters.");
        }
        if (getURI().trim().length() < 3) {
            throw new CombinedModelIntegrityException("URI of Class '" + getLabel() + "' must not be shorter than 3 characters.");
        }
        if (instance != null) {
            instance.validate();
        }
    }

    public boolean isMapped(){
        return getInstance() != null && getInstance().getSyntaxNodeUuid() != null;
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