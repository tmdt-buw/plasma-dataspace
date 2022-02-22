package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SemanticModel implements Serializable {

    private final List<SemanticModelNode> nodes;
    private final List<Relation> edges;
    /**
     * Knowledge base ID, set once sent to the KGS.
     */
    private String id;

    @JsonCreator
    public SemanticModel() {
        this(null, null, null);
    }

    @JsonCreator
    public SemanticModel(
            @Nullable @JsonProperty String id,
            @Nullable @JsonProperty List<SemanticModelNode> nodes,
            @Nullable @JsonProperty List<Relation> edges
    ) {
        this.id = id;
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
        this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
    }

    /* Getter */
    @JsonProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public List<SemanticModelNode> getNodes() {
        return nodes;
    }

    @JsonProperty
    public List<Relation> getEdges() {
        return edges;
    }

    /**
     * Retrieves a {@link CombinedModelElement} from the model.
     *
     * @param elementUuid The element's uuid
     * @return a matching element or null if not found
     */
    @JsonIgnore
    public SemanticModelNode getNode(@NotNull String elementUuid) {
        return getNodes().stream()
                .filter(schemaNode -> elementUuid.equals(schemaNode.getUuid()))
                .findFirst().orElseThrow();
    }

    /**
     * Retrieves a {@link Relation} from the model.
     *
     * @param relationUuid The relation uuid
     * @return a matching relation or null if not found
     */
    @JsonIgnore
    public Relation getRelation(@NotNull String relationUuid) {
        return getEdges().stream()
                .filter(edge -> relationUuid.equals(edge.getUuid()))
                .findFirst().orElseThrow();
    }

    /**
     * Validates the internal integrity of the {@link SemanticModel}
     * Checks that:
     * 1. All {@link Instance}s have an {@link SemanticClass} assigned
     * 2. All Relations have valid (existing and unique) 'from' and 'to' ids matching existing EntityType ids
     *
     * @throws CombinedModelIntegrityException If one or more errors occur
     */
    public void validate() throws CombinedModelIntegrityException {
        List<String> errors = new ArrayList<>();
        for (SemanticModelNode smn : getNodes()) {
            try {
                smn.validate();
            } catch (CombinedModelIntegrityException cmie) {
                errors.add(cmie.getMessage());
            }
        }
        for (Relation edge : getEdges()) {
            try {
                edge.validate();
            } catch (CombinedModelIntegrityException cmie) {
                errors.add(cmie.getMessage());
            }
            long fromCount = getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), edge.getFromId())).count();
            if (fromCount != 1) {
                errors.add("Relation " + edge.getUuid() + " fromId matches " + fromCount + " EntityTypes (must be 1)");
            }
            long toCount = getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), edge.getToId())).count();
            if (toCount != 1) {
                errors.add("Relation " + edge.getUuid() + " toId matches " + toCount + " EntityTypes (must be 1)");
            }
        }
        if (!errors.isEmpty()) {
            String errorMessage = String.join("\n", errors);
            throw new CombinedModelIntegrityException(errorMessage);
        }
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity with also the {@link Instance}s and {@link Relation}s cloned
     */
    public SemanticModel copy() {
        return new SemanticModel(
                getId(),
                CollectionUtilities.map(getNodes(),
                        SemanticModelNode::copy,
                        ArrayList::new),
                CollectionUtilities.map(getEdges(),
                        Relation::copy,
                        ArrayList::new)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        SemanticModel that = (SemanticModel) o;
        return Objects.equals(nodes, that.nodes) &&
                Objects.equals(edges, that.edges);
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"" + this.getClass().getSimpleName() + "\""
                + ", \"@super\":" + super.toString()
                + ", \"nodes\":" + StringUtilities.listToJson(nodes)
                + ", \"edges\":" + StringUtilities.listToJson(edges)
                + '}';
    }


}