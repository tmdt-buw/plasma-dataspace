package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A composite node is existed as a primitive node but was splitted using one or more {@link Splitting} objects.
 */
@JsonTypeName("CompositeNode")
public class CompositeNode extends SchemaNode {

    private static final long serialVersionUID = 5858068735685107958L;

    @NotNull
    private List<Splitting> splitter;
    private List<String> examples;
    private String cleansingPattern;

    public CompositeNode(
            @NotNull String label,
            @NotNull List<SyntacticOperationDTO> operations,
            boolean isValid,
            @Nullable String cleansingPattern,
            @NotNull List<Splitting> splitter
    ) {
        super(label, operations, isValid);
        this.cleansingPattern = cleansingPattern;
        this.splitter = splitter;
    }

    @JsonCreator
    public CompositeNode(
            @NotNull @JsonProperty String uuid,
            @JsonProperty @NotNull String label,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate,
            @NotNull @JsonProperty List<SyntacticOperationDTO> operations,
            @JsonProperty boolean isValid,
            @JsonProperty List<String> examples,
            @JsonProperty String cleansingPattern,
            @NotNull @JsonProperty List<Splitting> splitter
    ) {
        super(uuid, label, xCoordinate, yCoordinate, operations, isValid);
        this.examples = examples;
        this.cleansingPattern = cleansingPattern;
        this.splitter = splitter;
    }

    @JsonProperty
    public String getCleansingPattern() {
        return cleansingPattern;
    }

    public void setCleansingPattern(String cleansingPattern) {
        this.cleansingPattern = cleansingPattern;
    }

    @NotNull
    @JsonProperty
    public List<Splitting> getSplitter() {
        return splitter;
    }

    @JsonProperty
    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public void setSplitter(List<Splitting> splitter) {
        this.splitter = splitter;
    }

    @Override
    public CompositeNode copy() {
        return new CompositeNode(
                getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate(),
                CollectionUtilities.map(getOperations(),
                        SyntacticOperationDTO::copy,
                        ArrayList::new),
                isValid(),
                getExamples(),
                getCleansingPattern(),
                CollectionUtilities.map(getSplitter(),
                        Splitting::copy,
                        ArrayList::new));

    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"CompositeNode\""
                + ", \"@super\":" + super.toString()
                + ", \"cleansingPattern\":\"" + cleansingPattern + '"'
                + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cleansingPattern);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return super.equals(o);
    }
}
