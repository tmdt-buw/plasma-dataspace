package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.LinkableSyntaxNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Literal.class, name = "Literal")
})
public abstract class MappableSemanticNode extends LinkableSemanticNode {

    public static final String MAPPED_SYNTAX_NODE_PATH_PROPERTY = "syntaxPath";

    /**
     * The path in the original data schema to identify this node.
     * This may only be set when {@link LinkableSemanticNode:mappedSyntaxNodeUuid} is set also.
     * Form is expressed as a JSONPath.
     */
    private String mappedSyntaxNodePath;

    public MappableSemanticNode(@Nullable String uri, @NotNull String label) {
        super(uri, label, null, null, null);
    }

    public MappableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        super(uri, label, xCoordinate, yCoordinate, null);
    }

    public MappableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate, @Nullable String uuid) {
        super(uri, label, xCoordinate, yCoordinate, uuid);
    }

    public MappableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate, String mappedSyntaxNodeUuid, String mappedSyntaxNodeLabel, String mappedSyntaxNodePath, @Nullable String uuid) {
        super(uri, label, xCoordinate, yCoordinate, uuid);
        setMappedSyntaxNodeUuid(mappedSyntaxNodeUuid);
        setMappedSyntaxNodeLabel(mappedSyntaxNodeLabel);
        setMappedSyntaxNodePath(mappedSyntaxNodePath);
    }

    @JsonProperty(MAPPED_SYNTAX_NODE_PATH_PROPERTY)
    public String getMappedSyntaxNodePath() {
        return mappedSyntaxNodePath;
    }

    public void setMappedSyntaxNodePath(String mappedSyntaxNodePath) {
        this.mappedSyntaxNodePath = mappedSyntaxNodePath;
    }

    @Override
    public boolean isMapped() {
        return getMappedSyntaxNodePath() != null;
    }

    public void unmap() {
        setMappedSyntaxNodeUuid(null);
        setMappedSyntaxNodeLabel(null);
        setMappedSyntaxNodePath(null);
    }
}
