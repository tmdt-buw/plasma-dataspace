package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeName("PrimitiveNode")
public class PrimitiveNode extends SchemaNode {

    private static final long serialVersionUID = -144653268470507986L;

    private String cleansingPattern;
    private List<String> examples;
    private DataType dataType;

    public PrimitiveNode(
            @NotNull String label,
            @NotNull DataType dataType
    ) {
        this(label, null, true, dataType, null, null);
    }

    public PrimitiveNode(
            @NotNull String label,
            boolean isValid,
            @NotNull DataType dataType
    ) {
        this(label, null, isValid, dataType, null, null);
    }

    public PrimitiveNode(
            @NotNull String label,
            @Nullable List<SyntacticOperationDTO> operations,
            boolean isValid,
            @NotNull DataType dataType,
            @Nullable List<String> examples,
            @Nullable String cleansingPattern
    ) {
        super(label, operations == null ? new ArrayList<>() : operations, isValid);
        this.dataType = dataType;
        this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
        this.cleansingPattern = cleansingPattern;
    }

    @JsonCreator
    public PrimitiveNode(
            @NotNull @JsonProperty String uuid,
            @JsonProperty @NotNull String label,
            @JsonProperty Double xCoordinate,
            @JsonProperty Double yCoordinate,
            @NotNull @JsonProperty List<SyntacticOperationDTO> operations,
            @JsonProperty boolean isValid,
            @NotNull @JsonProperty DataType dataType,
            @JsonProperty List<String> examples,
            @JsonProperty String cleansingPattern
    ) {
        super(uuid, label, xCoordinate, yCoordinate, operations, isValid);
        this.dataType = dataType;
        this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
        this.cleansingPattern = cleansingPattern;
    }

    @JsonProperty
    public String getCleansingPattern() {
        return cleansingPattern;
    }

    public void setCleansingPattern(String cleansingPattern) {
        this.cleansingPattern = cleansingPattern;
    }

    @JsonProperty
    public List<String> getExamples() {
        return examples;
    }

    @JsonProperty
    public DataType getDataType() {
        return dataType;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public PrimitiveNode copy() {
        return new PrimitiveNode(
                getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate(),
                CollectionUtilities.map(getOperations(),
                        SyntacticOperationDTO::copy,
                        ArrayList::new),
                isValid(),
                getDataType(),
                getExamples(),
                getCleansingPattern());
    }


    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"PrimitiveNode\""
                + ", \"@super\":" + super.toString()
                + ", \"cleansingPattern\":\"" + cleansingPattern + '"'
                + ", \"examples\":" + StringUtilities.listToJson(examples)
                + ", \"dataType\":\"" + dataType + '"'
                + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cleansingPattern, dataType);
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }
}
