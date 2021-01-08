package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

public class SemanticModel implements Serializable {

	public static final String ENTITY_ID_PROPERTY = "entityId";
	public static final String NODES_PROPERTY = "nodes";
	private static final String EDGES_PROPERTY = "edges";

	/**
	 * Knowledge base ID, set once sent to the KGS.
	 */
	private String entityId;
	private final List<EntityType> nodes;
	private final List<Relation> edges;

	@JsonCreator
	public SemanticModel(
			@Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
			@JsonProperty(NODES_PROPERTY) List<EntityType> nodes,
			@JsonProperty(EDGES_PROPERTY) List<Relation> edges
	) {
		this.entityId = entityId;
		this.nodes = nodes != null ? new ArrayList<>(nodes) : Collections.emptyList();
		this.edges = edges != null ? new ArrayList<>(edges) : Collections.emptyList();
	}

	/* Getter */
	@JsonProperty(ENTITY_ID_PROPERTY)
	public String getEntityId() {
		return entityId;
	}

	@JsonProperty(NODES_PROPERTY)
	public List<EntityType> getNodes() {
		return nodes;
	}

	@JsonProperty(EDGES_PROPERTY)
	public List<Relation> getEdges() {
		return edges;
	}

	/**
	 * Returns a copy of this model where similar concepts are merged to one.
	 * This results in two {@link EntityType}s that reference {@link EntityConcept}s for which
	 * {@link EntityConcept#sameAs(EntityConcept)} is {@code true}, those will be merged and the reference from
	 * all subsequent {@link EntityType} instances will be changed to reference the only {@link EntityConcept} instance
	 * for the given {@code sourceURI}.
	 * Procedure for {@link Relation}s and {@link RelationConcept}s is analogous.
	 *
	 * @return A copy of the current {@link SemanticModel} with merged same {@link EntityConcept}s and {@link RelationConcept}s
	 */
	@JsonIgnore
	public SemanticModel mergeConcepts() {
        SemanticModel model = copy();
        // merge all entity concepts
        HashMap<String, EntityConcept> seenConcepts = new HashMap<>();
        model.getNodes().forEach(et -> {
            if (seenConcepts.containsKey(et.getEntityConcept().getSourceURI())) {
                EntityConcept ec = seenConcepts.get(et.getEntityConcept().getSourceURI());
                et.setEntityConcept(ec);
            } else {
                seenConcepts.put(et.getEntityConcept().getSourceURI(), et.getEntityConcept());
            }
		});
		// merge all relation concepts
		HashMap<String, RelationConcept> seenRelations = new HashMap<>();
		model.getEdges().forEach(r -> {
			if (seenRelations.containsKey(r.getConcept().getSourceURI())) {
				RelationConcept rc = seenRelations.get(r.getConcept().getSourceURI());
				r.setConcept(rc);
			} else {
				seenRelations.put(r.getConcept().getSourceURI(), r.getConcept());
			}
		});
		return model;
    }

	/**
	 * Retrieves an {@link EntityType} from the model.
	 *
	 * @param entityTypeUuid The types uuid
	 * @return a matching type or null if not found
	 */
	@JsonIgnore
	public EntityType getEntityType(@NotNull String entityTypeUuid) {
		return getNodes().stream()
				.filter(schemaNode -> entityTypeUuid.equals(schemaNode.getUuid()))
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
	 * Validates the interal integrity of the {@link SemanticModel}
	 * Checks that:
	 * 1. All {@link EntityType}s have an {@link EntityConcept} assigend
	 * 2. All Relations have valid (existing and unique) 'from' and 'to' ids matching existing EntityType ids
	 * 3. All Relations have a {@link RelationConcept} assigned
	 *
	 * @throws CombinedModelIntegrityException If one or more errors occur
	 */
	public void validate() throws CombinedModelIntegrityException {
		List<String> errors = new ArrayList<>();
		for (EntityType entityType : getNodes()) {
			EntityConcept entityConcept = entityType.getEntityConcept();
			if (entityConcept == null) {
				errors.add("EntityType " + entityType.getUuid() + " has no EntityConcept assigned");
			}
		}
		for (Relation edge : getEdges()) {
			long fromCount = getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), edge.getFromId())).count();
			if (fromCount != 1) {
				errors.add("Relation " + edge.getUuid() + " fromId matches " + fromCount + " EntityTypes (must be 1)");
			}
			long toCount = getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), edge.getToId())).count();
			if (toCount != 1) {
				errors.add("Relation " + edge.getUuid() + " toId matches " + toCount + " EntityTypes (must be 1)");
			}
			RelationConcept concept = edge.getConcept();
			if (concept == null) {
				errors.add("Relation " + edge.getUuid() + " has no RelationConcept assigned");
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
     * @return The cloned entity with also the {@link EntityType}s and {@link Relation}s cloned
     */
    public SemanticModel copy() {
        return new SemanticModel(
                getEntityId(),
                CollectionUtilities.map(getNodes(),
                        EntityType::copy,
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
		return 		       Objects.equals(nodes, that.nodes) &&
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