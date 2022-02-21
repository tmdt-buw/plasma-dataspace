package de.buw.tmdt.plasma.datamodel.modification;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Defines a Delta recommendation, a partial update of a combined model.
 */
public class DeltaModification implements Serializable {

    /**
     * A unique id identifying this recommendation.
     */
    @NotNull
    private final String reference;

    /**
     * Indicates if this modification removes elements.
     */
    private boolean deletion = false;
    /**
     * The set of modified semantic entities.
     */
    @NotNull
    private Set<SemanticModelNode> entities;
    /**
     * The set of modified semantic relations.
     */
    @NotNull
    private Set<Relation> objectProperties;
    /**
     * The set of modified syntactic nodes.
     */
    @NotNull
    private Set<SchemaNode> nodes;
    /**
     * The set of modified syntactic edges.
     */
    @NotNull
    private Set<Edge> edges;

    /**
     * Lists the ids of anchor elements.
     * An anchor is a node that belongs to the current state of the model and serves as a connection point
     * for this modification. This list might be empty, e.g. in case a single new semantic node is added.
     */
    @NotNull
    private List<String> anchors;

    private Double confidence;

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<SemanticModelNode> entities,
                             @Nullable Set<Relation> objectProperties,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges) {
        this(reference, false, entities, objectProperties, nodes, edges, null, 0.0);
    }

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<SemanticModelNode> entities,
                             @Nullable Set<Relation> objectProperties,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges,
                             @Nullable Double confidence) {
        this(reference, false, entities, objectProperties, nodes, edges, null, confidence);
    }

    @JsonCreator
    public DeltaModification(@NotNull String reference,
                             boolean deletion,
                             @Nullable Set<SemanticModelNode> entities,
                             @Nullable Set<Relation> objectProperties,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges,
                             @Nullable List<String> anchors,
                             @Nullable Double confidence) {
        this.reference = reference;
        this.deletion = deletion;
        this.entities = entities == null ? new HashSet<>() : entities;
        this.objectProperties = objectProperties == null ? new HashSet<>() : objectProperties;
        this.nodes = nodes == null ? new HashSet<>() : nodes;
        this.edges = edges == null ? new HashSet<>() : edges;
        this.anchors = anchors == null ? new ArrayList<>() : anchors;
        this.confidence = confidence;
    }

    @NotNull
    public String getReference() {
        return reference;
    }

    public boolean isDeletion() {
        return deletion;
    }

    @NotNull
    public Set<SemanticModelNode> getEntities() {
        return entities;
    }

    public void setEntities(@NotNull Set<SemanticModelNode> entities) {
        this.entities = entities;
    }

    @NotNull
    public Set<SchemaNode> getNodes() {
        return nodes;
    }

    @NotNull
    public Set<Edge> getEdges() {
        return edges;
    }

    @NotNull
    public List<String> getAnchors() {
        return anchors;
    }

    @Nullable
    public Double getConfidence() {
        return confidence;
    }

    public void setDeletion(boolean deletion) {
        this.deletion = deletion;
    }

    @NotNull
    public Set<Relation> getRelations() {
        return objectProperties;
    }

    public void setRelations(@NotNull Set<Relation> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public void setNodes(@NotNull Set<SchemaNode> nodes) {
        this.nodes = nodes;
    }

    public void setEdges(@NotNull Set<Edge> edges) {
        this.edges = edges;
    }

    public void setAnchors(@NotNull List<String> anchors) {
        this.anchors = anchors;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * Checks if a given id is set as an anchor for this modification.
     *
     * @param id The element id to check.
     * @return true if the id is listed as an anchor node, false otherwise
     */
    public boolean hasAnchor(String id) {
        return anchors.contains(id);
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity with also the {@link SemanticModelNode}s, {@link Relation}s, {@link SchemaNode}s and {@link Edge}s cloned
     */
    public DeltaModification copy() {
        return new DeltaModification(
                getReference(),
                isDeletion(),
                CollectionUtilities.map(getEntities(),
                        SemanticModelNode::copy,
                        HashSet::new),
                CollectionUtilities.map(getRelations(),
                        Relation::copy,
                        HashSet::new),
                CollectionUtilities.map(getNodes(),
                        SchemaNode::copy,
                        HashSet::new),
                CollectionUtilities.map(getEdges(),
                        Edge::copy,
                        HashSet::new),
                new ArrayList<>(anchors),
                getConfidence()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        de.buw.tmdt.plasma.datamodel.modification.DeltaModification that = (de.buw.tmdt.plasma.datamodel.modification.DeltaModification) o;
        return Objects.equals(getReference(), that.getReference());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReference());
    }

    @Override
    public String toString() {
        return "DeltaModification{" +
                "reference='" + reference + '\'' +
                ". deletion=" + isDeletion() +
                ", entities=" + getEntities().size() +
                ", relations=" + getRelations().size() +
                ", modes=" + getNodes().size() +
                ", edges=" + getEdges().size() +
                ", confidence=" + confidence +
                '}';
    }
}
