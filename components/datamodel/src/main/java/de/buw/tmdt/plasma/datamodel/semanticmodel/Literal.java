package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Literal extends SemanticModelNode {

    public static final String VALUE_PROPERTY = "value";
    public static final String SYNTAX_NODE_PROPERTY = "syntaxNodeUuid";

    private String syntaxNodeUuid;

    public Literal(String value) {
        super(null, value); // use the value as label
    }

    @JsonCreator
    public Literal(@Nullable @JsonProperty(UUID_PROPERTY) String uuid,
                   @NotNull @JsonProperty(VALUE_PROPERTY) String value,
                   @Nullable @JsonProperty(XCOORDINATE_PROPERTY) Double xCoordinate,
                   @Nullable @JsonProperty(YCOORDINATE_PROPERTY) Double yCoordinate,
                   @Nullable @JsonProperty(SYNTAX_NODE_PROPERTY) String syntaxNodeUuid) {
        super(uuid, null, value, xCoordinate, yCoordinate);
        this.syntaxNodeUuid = syntaxNodeUuid;
    }

    @Override
    public Literal copy() {
        return new Literal(getUuid(), getValue(), getXCoordinate(), getYCoordinate(), getSyntaxNodeUuid());
    }

    @Override
    public Literal instanciate() {
        return new Literal(null, getValue(), getXCoordinate(), getYCoordinate(), null);
    }

    @JsonProperty(VALUE_PROPERTY)
    public String getValue() {
        return getLabel();
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getValue() == null) {
            throw new CombinedModelIntegrityException("Literal " + getUuid() + " has no proper value");
        }
    }

    public boolean isMapped() {
        return getMappedSyntaxNodeId() != null;
    }

    @Override
    @JsonIgnore
    public String getMappedSyntaxNodeId() {
        return getSyntaxNodeUuid();
    }

    @Override
    public boolean sameAs(SemanticModelNode o) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass().equals(o.getClass())) {
            Literal that = (Literal) o;
            return Objects.equals(this.getUuid(), that.getUuid()) &&
                    sameAs(that);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Literal{" +
                "value='" + getLabel() + '\'' +
                '}';
    }

    @JsonProperty(SYNTAX_NODE_PROPERTY)
    public String getSyntaxNodeUuid() {
        return syntaxNodeUuid;
    }

    public void setSyntaxNodeUuid(String syntaxNodeUuid) {
        this.syntaxNodeUuid = syntaxNodeUuid;
    }
}
