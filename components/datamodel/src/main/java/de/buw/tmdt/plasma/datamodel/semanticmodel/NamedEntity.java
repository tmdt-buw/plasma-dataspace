package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonTypeName("NamedEntity")
public class NamedEntity extends SemanticModelNode implements URIDefinedElement {

    private String description;

    public NamedEntity(@NotNull String uri, @NotNull String label, @NotNull String description) {
        super(uri, label);
        this.description = description;
    }

    public NamedEntity(@NotNull String uri, @NotNull String label, @NotNull String description, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        super(uri, label, xCoordinate, yCoordinate);
        this.description = description;
    }

    @JsonCreator
    public NamedEntity(@Nullable String uuid, @NotNull String uri, @NotNull String label, @NotNull String description, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        super(uuid, uri, label, xCoordinate, yCoordinate);
        this.description = description;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public NamedEntity copy() {
        return new NamedEntity(getUuid(), getURI(), getLabel(), getDescription(), getXCoordinate(), getYCoordinate());
    }

    @Override
    public NamedEntity build() {
        return new NamedEntity(getURI(), getLabel(), getDescription(), getXCoordinate(), getYCoordinate());
    }

    @Override
    public void validate() throws CombinedModelIntegrityException {

        if (getURI() == null || getURI().isBlank()) {
            throw new CombinedModelIntegrityException("NamedEntity " + getUuid() + " has no proper URL assigned");
        }
        if (getDescription() == null || getDescription().isBlank()) {
            throw new CombinedModelIntegrityException("NamedEntity " + getUuid() + " has no proper description assigned");
        }
        if (getLabel().isBlank()) {
            throw new CombinedModelIntegrityException("NamedEntity" + getUuid() + " has no proper label assigned");
        }

    }


    @Override
    public String toString() {
        return "NamedEntity{" +
                "uri='" + getURI() + '\'' +
                "label='" + getLabel() + '\'' +
                "description='" + description.length() + '\'' +
                '}';
    }
}
