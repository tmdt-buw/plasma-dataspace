package de.buw.tmdt.plasma.datamodel.modification;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
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
    private static final String ENTITIES_PROPERTY = "entities";
    private static final String RELATIONS_PROPERTY = "relations";
    private static final String CONFIDENCE_PROPERTY = "confidence";

    /**
     * A unique id identifying this recommendation.
     */
    public String reference;
    /**
     * The set of modified semantic entities
     */
    public Set<EntityType> entities;
    /**
     * The set of modified semantic relations
     */
    public Set<Relation> relations;
    /**
     * The set of modified syntactic nodes
     */
    private Set<SchemaNode> nodes;
    /**
     * The set of modified syntactic edges
     */
    private  Set<Edge> edges;

    public Double confidence;

    public DeltaModification(@NotNull @JsonProperty(REFERENCE_PROPERTY) String reference,
                             @Nullable @JsonProperty(ENTITIES_PROPERTY) Set<EntityType> entities,
                             @Nullable @JsonProperty(RELATIONS_PROPERTY) Set<Relation> relations,
                             @Nullable @JsonProperty(NODES_PROPERTY) Set<SchemaNode> nodes,
                             @Nullable @JsonProperty(EDGES_PROPERTY)  Set<Edge> edges,
                             @Nullable @JsonProperty(CONFIDENCE_PROPERTY) Double confidence) {
        this.reference = reference;
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

    @Nullable
    @JsonProperty(ENTITIES_PROPERTY)
    public Set<EntityType> getEntities() {
        return entities;
    }

    @Nullable
    @JsonProperty(RELATIONS_PROPERTY)
    public Set<Relation> getRelations() {
        return relations;
    }

    @Nullable
    @JsonProperty(NODES_PROPERTY)
    public Set<SchemaNode> getNodes() {
        return nodes;
    }

    @Nullable
    @JsonProperty(EDGES_PROPERTY)
    public Set<Edge> getEdges() {
        return edges;
    }

    @Nullable
    @JsonProperty(CONFIDENCE_PROPERTY)
    public Double getConfidence() {
        return confidence;
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
        return "DeltaRecommendationDTO{" +
                "reference='" + reference + '\'' +
                ", entities=" + entities +
                ", relations=" + relations +
                ", confidence=" + confidence +
                '}';
    }
}
