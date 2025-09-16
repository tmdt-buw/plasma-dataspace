package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.LinkableSyntaxNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Linkable Semantic Nodes can be linked to a syntax node in the semantic modeling UI.
 * This link is only symbolic and does not result in a data mapping.
 * For a data-related mapping to a leaf node see {@link MappableSemanticNode}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Class.class, name = "Class")
})
public abstract class LinkableSemanticNode extends SemanticNode {

    public static final String MAPPED_SYNTAX_NODE_UUID_PROPERTY = "syntaxNodeUuid";
    public static final String MAPPED_SYNTAX_NODE_LABEL_PROPERTY = "syntaxLabel";
    public static final String MAPPED_SYNTAX_NODE_PATH_PROPERTY = "syntaxPath";

    // TODO fields need to be renamed to "Linked..."
    /**
     * The id of the syntax {@link LinkableSyntaxNode} this object is linked to.
     * Will be null if not linked.
     */
    private String mappedSyntaxNodeUuid;

    /**
     * The original label of an attribute if this instantiation is linked to one, null otherwise.
     * This may only be set when {@link #mappedSyntaxNodeUuid} is set also.
     */
    private String mappedSyntaxNodeLabel;

    /**
     * The path in the original data schema to identify this node.
     * This may only be set when {@link LinkableSemanticNode:mappedSyntaxNodeUuid} is set also.
     * Form is expressed as a JSONPath.
     */
    private String mappedSyntaxNodePath;

    public LinkableSemanticNode(@Nullable String uri, @NotNull String label) {
        super(uri, label, null, null, null);
    }

    public LinkableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate) {
        super(uri, label, xCoordinate, yCoordinate, null);
    }

    public LinkableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate, @Nullable String uuid) {
        super(uri, label, xCoordinate, yCoordinate, uuid);
    }

    public LinkableSemanticNode(@Nullable String uri, @NotNull String label, @Nullable Double xCoordinate, @Nullable Double yCoordinate, String mappedSyntaxNodeUuid, String mappedSyntaxNodeLabel,  String mappedSyntaxNodePath, @Nullable String uuid) {
        super(uri, label, xCoordinate, yCoordinate, uuid);
        setMappedSyntaxNodeUuid(mappedSyntaxNodeUuid);
        setMappedSyntaxNodeLabel(mappedSyntaxNodeLabel);
        setMappedSyntaxNodePath(mappedSyntaxNodePath);
    }

    @JsonProperty(MAPPED_SYNTAX_NODE_UUID_PROPERTY)
    public String getMappedSyntaxNodeUuid() {
        return mappedSyntaxNodeUuid;
    }

    public void setMappedSyntaxNodeUuid(String mappedSyntaxNodeUuid) {
        this.mappedSyntaxNodeUuid = mappedSyntaxNodeUuid;
    }

    @JsonProperty(MAPPED_SYNTAX_NODE_LABEL_PROPERTY)
    public String getMappedSyntaxNodeLabel() {
        return mappedSyntaxNodeLabel;
    }

    public void setMappedSyntaxNodeLabel(String mappedSyntaxNodeLabel) {
        this.mappedSyntaxNodeLabel = mappedSyntaxNodeLabel;
    }

    @JsonProperty(MAPPED_SYNTAX_NODE_PATH_PROPERTY)
    public String getMappedSyntaxNodePath() {
        return mappedSyntaxNodePath;
    }

    public void setMappedSyntaxNodePath(String mappedSyntaxNodePath) {
        this.mappedSyntaxNodePath = mappedSyntaxNodePath;
    }

    @Override
    public boolean isLinked() {
        return getMappedSyntaxNodeUuid() != null;
    }

    public void unmap() {
        setMappedSyntaxNodeUuid(null);
        setMappedSyntaxNodeLabel(null);
    }
}
