package de.buw.tmdt.plasma.datamodel.modification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel.EDGES_PROPERTY;
import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel.NODES_PROPERTY;

/**
 * Defines a Delta recommendation, a partial update of a combined model.
 */
public class DeltaModification implements Serializable {

    private static final String REFERENCE_PROPERTY = "reference";
    private static final String DELETION_PROPERTY = "deletion";
    private static final String ENTITIES_PROPERTY = "entities";
    private static final String RELATIONS_PROPERTY = "relations";
    private static final String ANCHORS_PROPERTY = "anchors";
    private static final String CONFIDENCE_PROPERTY = "confidence";

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
    private Set<EntityType> entities;
    /**
     * The set of modified semantic relations.
     */
    private Set<Relation> relations;
    /**
     * The set of modified syntactic nodes.
     */
    private Set<SchemaNode> nodes;
    /**
     * The set of modified syntactic edges.
     */
    private Set<Edge> edges;

    /**
     * Lists the ids of anchor elements.
     * An anchor is a node that belongs to the current state of the model and serves as a connection point
     * for this modification. This list might be empty, e.g. in case a single new semantic node is added.
     */
    private List<String> anchors;

    private Double confidence;

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<EntityType> entities,
                             @Nullable Set<Relation> relations,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges) {
        this(reference, false, entities, relations, nodes, edges, null, 0.0);
    }

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<EntityType> entities,
                             @Nullable Set<Relation> relations,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges,
                             @Nullable Double confidence) {
        this(reference, false, entities, relations, nodes, edges, null, confidence);
    }

    @JsonCreator
    public DeltaModification(@NotNull @JsonProperty(REFERENCE_PROPERTY) String reference,
                             @JsonProperty(DELETION_PROPERTY) boolean deletion,
                             @Nullable @JsonProperty(ENTITIES_PROPERTY) Set<EntityType> entities,
                             @Nullable @JsonProperty(RELATIONS_PROPERTY) Set<Relation> relations,
                             @Nullable @JsonProperty(NODES_PROPERTY) Set<SchemaNode> nodes,
                             @Nullable @JsonProperty(EDGES_PROPERTY) Set<Edge> edges,
                             @Nullable @JsonProperty(ANCHORS_PROPERTY) List<String> anchors,
                             @Nullable @JsonProperty(CONFIDENCE_PROPERTY) Double confidence) {
        this.reference = reference;
        this.deletion = deletion;
        this.entities = entities == null ? new HashSet<>() : entities;
        this.relations = relations == null ? new HashSet<>() : relations;
        this.nodes = nodes == null ? new HashSet<>() : nodes;
        this.edges = edges == null ? new HashSet<>() : edges;
        this.anchors = anchors == null ? new ArrayList<>() : anchors;
        this.confidence = confidence;
    }

    @NotNull
    @JsonProperty(REFERENCE_PROPERTY)
    public String getReference() {
        return reference;
    }

    @JsonProperty(DELETION_PROPERTY)
    public boolean isDeletion() {
        return deletion;
    }

    @NotNull
    @JsonProperty(ENTITIES_PROPERTY)
    public Set<EntityType> getEntities() {
        return entities;
    }

    @NotNull
    @JsonProperty(RELATIONS_PROPERTY)
    public Set<Relation> getRelations() {
        return relations;
    }

    @NotNull
    @JsonProperty(NODES_PROPERTY)
    public Set<SchemaNode> getNodes() {
        return nodes;
    }

    @NotNull
    @JsonProperty(EDGES_PROPERTY)
    public Set<Edge> getEdges() {
        return edges;
    }

    @NotNull
    @JsonProperty(ANCHORS_PROPERTY)
    public List<String> getAnchors() {
        return anchors;
    }

    @Nullable
    @JsonProperty(CONFIDENCE_PROPERTY)
    public Double getConfidence() {
        return confidence;
    }

    public void setDeletion(boolean deletion) {
        this.deletion = deletion;
    }

    public void setEntities(Set<EntityType> entities) {
        this.entities = entities;
    }

    public void setRelations(Set<Relation> relations) {
        this.relations = relations;
    }

    public void setNodes(Set<SchemaNode> nodes) {
        this.nodes = nodes;
    }

    public void setEdges(Set<Edge> edges) {
        this.edges = edges;
    }

    public void setAnchors(List<String> anchors) {
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
     * @return The cloned entity with also the {@link EntityType}s, {@link Relation}s, {@link SchemaNode}s and {@link Edge}s cloned
     */
    public DeltaModification copy() {
        return new DeltaModification(
                getReference(),
                isDeletion(),
                CollectionUtilities.map(getEntities(),
                        EntityType::copy,
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
        DeltaModification that = (DeltaModification) o;
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
