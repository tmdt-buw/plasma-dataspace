package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SemanticModel {

	private static final long serialVersionUID = 8522383284345885691L;

	public static final String NODES_PROPERTY = "nodes";
	private static final String EDGES_PROPERTY = "edges";

	private final List<EntityType> nodes;
	private final List<Relation> edges;

	@JsonCreator
	public SemanticModel(
			@JsonProperty(NODES_PROPERTY) List<EntityType> nodes,
			@JsonProperty(EDGES_PROPERTY) List<Relation> edges
	) {
		this.nodes = nodes != null ? new ArrayList<>(nodes) : Collections.emptyList();
		this.edges = edges != null ? new ArrayList<>(edges) : Collections.emptyList();
	}

	@JsonProperty(NODES_PROPERTY)
	public List<EntityType> getNodes() {
		return nodes;
	}

	@JsonProperty(EDGES_PROPERTY)
	public List<Relation> getEdges() {
		return edges;
	}

	@Override
	public int hashCode() {
		return Objects.hash( nodes, edges);
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
		return "{\"@class\":\"SemanticModelDTO\""
		       + ", \"@super\":" + super.toString()
		       + ", \"nodes\":" + StringUtilities.listToJson(nodes)
		       + ", \"edges\":" + StringUtilities.listToJson(edges)
		       + '}';
	}
}