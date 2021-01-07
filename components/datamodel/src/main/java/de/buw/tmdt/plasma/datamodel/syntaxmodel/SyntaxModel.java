package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SyntaxModel implements Serializable {

	private static final String ROOT_PROPERTY = "root";
	public static final String NODES_PROPERTY = "nodes";
	public static final String EDGES_PROPERTY = "edges";
	private static final long serialVersionUID = -7924812162247359979L;

	private final String root;
	private final ArrayList<SchemaNode> nodes;
	private final ArrayList<Edge> edges;


	public SyntaxModel(
			@JsonProperty(ROOT_PROPERTY) @NotNull String root,
			@JsonProperty(NODES_PROPERTY) @NotNull List<SchemaNode> nodes,
			@JsonProperty(EDGES_PROPERTY) @NotNull List<Edge> edges
	) {
		this.root = root;
		if (CollectionUtilities.containsNull(nodes)) {
			throw new IllegalArgumentException("Nodes must not contain null.");
		}
		this.nodes = new ArrayList<>(nodes);
		if (CollectionUtilities.containsNull(edges)) {
			throw new IllegalArgumentException("Edges must not contain null.");
		}
		this.edges = new ArrayList<>(edges);
	}

	@JsonProperty(ROOT_PROPERTY)
	public String getRoot() {
		return root;
	}

	@JsonProperty(NODES_PROPERTY)
	public List<SchemaNode> getNodes() {
		return nodes;
	}

	@JsonProperty(EDGES_PROPERTY)
	public List<Edge> getEdges() {
		return edges;
	}

    @NotNull
    @JsonIgnore
    public SchemaNode getParentNodeFor(String nodeUuid) {
        Edge edgeToParent = getEdges().stream()
                .filter(edge -> edge.getTo().equals(nodeUuid))
                .findFirst().orElseThrow();
        return getNodes().stream()
                .filter(schemaNode -> schemaNode.getUuid().equals(edgeToParent.getFrom()))
                .findFirst().orElseThrow();
    }

    @NotNull
    @JsonIgnore
    public List<SchemaNode> getChildNodesForNode(String nodeUuid) {
	    List<String> childNodeUuids = getEdges().stream()
			    .filter(edge -> edge.getFrom().equals(nodeUuid))
			    .map(Edge::getTo)
			    .collect(Collectors.toList());
	    return getNodes().stream()
			    .filter(schemaNode -> childNodeUuids.contains(schemaNode.getUuid()))
			    .collect(Collectors.toList());
    }

	@JsonIgnore
	public SchemaNode getNode(@NotNull String nodeUuid) {
		return getNodes().stream()
				.filter(schemaNode -> nodeUuid.equals(schemaNode.getUuid()))
				.findFirst().orElse(null);
	}

	/**
	 * Creates a deep clone of the current entity.
	 *
	 * @return The cloned entity with also the {@link SchemaNode}s and {@link Edge}s cloned
	 */
	public SyntaxModel copy() {
		return new SyntaxModel(
				getRoot(),
				CollectionUtilities.map(getNodes(),
						SchemaNode::copy,
						ArrayList::new),
				CollectionUtilities.map(getEdges(),
						Edge::copy,
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
		SyntaxModel that = (SyntaxModel) o;
		return Objects.equals(nodes, that.nodes) &&
		       Objects.equals(edges, that.edges) &&
		       Objects.equals(root, that.root);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"" + this.getClass().getSimpleName() + "\""
				+ ", \"root\":" + root
				+ ", \"nodes\":" + StringUtilities.listToJson(nodes)
				+ ", \"edges\":" + StringUtilities.listToJson(edges)
				+ '}';
	}
}
