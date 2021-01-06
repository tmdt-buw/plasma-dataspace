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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel.EDGES_PROPERTY;
import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel.NODES_PROPERTY;

/**
 * Defines a Delta recommendation, a partial update of a combined model.
 */
public class DeltaModification implements Serializable, Cloneable {

    private static final String REFERENCE_PROPERTY = "reference";
    private static final String DELETION_PROPERTY = "deletion";
    private static final String ENTITIES_PROPERTY = "entities";
    private static final String RELATIONS_PROPERTY = "relations";
    private static final String CONFIDENCE_PROPERTY = "confidence";

    /**
     * A unique id identifying this recommendation.
     */
    private String reference;

    /**
     * Indicates if this modification removes elements
     */
    private boolean deletion = false;
    /**
     * The set of modified semantic entities
     */
    private Set<EntityType> entities;
    /**
     * The set of modified semantic relations
     */
    private Set<Relation> relations;
    /**
     * The set of modified syntactic nodes
     */
    private Set<SchemaNode> nodes;
    /**
     * The set of modified syntactic edges
     */
    private Set<Edge> edges;

    private Double confidence;

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<EntityType> entities,
                             @Nullable Set<Relation> relations,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges) {
        this(reference, entities, relations, nodes, edges, 0.0);
    }

    public DeltaModification(@NotNull String reference,
                             @Nullable Set<EntityType> entities,
                             @Nullable Set<Relation> relations,
                             @Nullable Set<SchemaNode> nodes,
                             @Nullable Set<Edge> edges,
                             @Nullable Double confidence) {
        this.reference = reference;
        this.entities = entities == null ? new HashSet<>() : entities;
        this.relations = relations == null ? new HashSet<>() : relations;
        this.nodes = nodes == null ? new HashSet<>() : nodes;
        this.edges = edges == null ? new HashSet<>() : edges;
        this.confidence = confidence;
    }

    @JsonCreator
    public DeltaModification(@NotNull @JsonProperty(REFERENCE_PROPERTY) String reference,
                             @JsonProperty(DELETION_PROPERTY) boolean deletion,
                             @Nullable @JsonProperty(ENTITIES_PROPERTY) Set<EntityType> entities,
                             @Nullable @JsonProperty(RELATIONS_PROPERTY) Set<Relation> relations,
                             @Nullable @JsonProperty(NODES_PROPERTY) Set<SchemaNode> nodes,
                             @Nullable @JsonProperty(EDGES_PROPERTY) Set<Edge> edges,
                             @Nullable @JsonProperty(CONFIDENCE_PROPERTY) Double confidence) {
        this.reference = reference;
        this.deletion = deletion;
        this.entities = entities == null ? new HashSet<>() : entities;
        this.relations = relations == null ? new HashSet<>() : relations;
        this.nodes = nodes == null ? new HashSet<>() : nodes;
        this.edges = edges == null ? new HashSet<>() : edges;
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

    @JsonProperty(ENTITIES_PROPERTY)
    public Set<EntityType> getEntities() {
        return entities;
    }

    @JsonProperty(RELATIONS_PROPERTY)
    public Set<Relation> getRelations() {
        return relations;
    }

    @JsonProperty(NODES_PROPERTY)
    public Set<SchemaNode> getNodes() {
        return nodes;
    }

    @JsonProperty(EDGES_PROPERTY)
    public Set<Edge> getEdges() {
        return edges;
    }

    @Nullable
    @JsonProperty(CONFIDENCE_PROPERTY)
    public Double getConfidence() {
        return confidence;
    }

    public void setReference(String reference) {
        this.reference = reference;
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

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * Creates a deep clone of the current entity
     *
     * @return The cloned entity with also the {@link EntityType}s, {@link Relation}s, {@link SchemaNode}s and {@link Edge}s cloned
     */
    public DeltaModification clone() {
        return new DeltaModification(
                getReference(),
                isDeletion(),
                CollectionUtilities.map(getEntities(),
                        EntityType::clone,
                        HashSet::new),
                CollectionUtilities.map(getRelations(),
                        Relation::clone,
                        HashSet::new),
                CollectionUtilities.map(getNodes(),
                        SchemaNode::clone,
                        HashSet::new),
                CollectionUtilities.map(getEdges(),
                        Edge::clone,
                        HashSet::new),
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
                ", entities=" + entities.size() +
                ", relations=" + relations.size() +
                ", modes=" + (getNodes() != null ? getNodes().size() : 0) +
                ", edges=" + (getEdges() != null ? getEdges().size() : 0) +
                ", confidence=" + confidence +
                '}';
    }
}
