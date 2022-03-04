package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Literal extends SemanticModelNode {

    public static final String VALUE_PROPERTY = "value";

    private String value;

    public Literal(String value) {
        super(null, value); // use the value as label
        this.value = value;
    }

    @JsonCreator
    public Literal(@Nullable @JsonProperty(UUID_PROPERTY) String uuid,
                   @NotNull @JsonProperty(LABEL_PROPERTY) String value,
                   @Nullable @JsonProperty(XCOORDINATE_PROPERTY) Double xCoordinate,
                   @Nullable @JsonProperty(YCOORDINATE_PROPERTY) Double yCoordinate) {
        super(uuid, null, value, xCoordinate, yCoordinate);
    }

    @Override
    public Literal copy() {
        return new Literal(getUuid(), getValue(), getXCoordinate(), getYCoordinate());
    }

    @Override
    public Literal instanciate() {
        return new Literal(null, getValue(), getXCoordinate(), getYCoordinate());
    }

    @JsonProperty(VALUE_PROPERTY)
    public String getValue() {
        return value;
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getValue() == null || getValue().isBlank()) {
            throw new CombinedModelIntegrityException("Literal " + getUuid() + " has no has no proper value");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            Literal that = (Literal) o;
            return Objects.equals(this.getURI(), that.getURI()) &&
                    Objects.equals(this.getLabel(), that.getLabel()) &&
                    Objects.equals(this.getXCoordinate(), that.getXCoordinate()) &&
                    Objects.equals(this.getYCoordinate(), that.getYCoordinate());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Literal{" +
                "value='" + value + '\'' +
                '}';
    }
}
