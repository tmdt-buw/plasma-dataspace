package de.buw.tmdt.plasma.services.sas.core.converter;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Splitting;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.sas.core.model.syntaxmodel.*;
import de.buw.tmdt.plasma.services.sas.core.model.syntaxmodel.members.*;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts the local {@link de.buw.tmdt.plasma.services.sas.core.model.syntaxmodel.Node} tree to
 * the shared {@link CombinedModel}, or {@link SyntaxModel} respectively.
 */
@Service
public class CombinedModelConverter {

	private static final Logger logger = LoggerFactory.getLogger(CombinedModelConverter.class);
	private static final String ROOT_ELEMENT_LABEL = "ROOT";
	private static final String ROOT_PATH_TOKEN = "";
	private static final String COLLISION_ELEMENT_LABEL = "COLLISION";

	@Autowired
	public CombinedModelConverter() {

	}

	/**
	 * Conversion methods internal model ===>  CombinedModel
	 * @param root The root element of the internal tree
	 * @return The resulting {@link SyntaxModel}
	 */
	@NotNull
	public SyntaxModel toCombinedModel(@NotNull Node root) {
		@NotNull SerializationContext serializationContext = new SerializationContext();
		SchemaNode syntaxRoot;
		List<String> path = new ArrayList<>();
		path.add(ROOT_PATH_TOKEN);
		if (root instanceof SetNode) {
			syntaxRoot = toCombinedModel((SetNode) root, ROOT_ELEMENT_LABEL, path, serializationContext);
		} else if (root instanceof ObjectNode) {
			syntaxRoot = toCombinedModel((ObjectNode) root, ROOT_ELEMENT_LABEL, path, serializationContext);
		} else {
			throw new IllegalArgumentException(root.toString());
		}
		serializationContext.nodeLookUp.put(root.getUuid().toString(), syntaxRoot);
		return new SyntaxModel(
				syntaxRoot.getUuid(),
				new ArrayList<>(serializationContext.nodeLookUp.values()),
				serializationContext.edges
		);
	}

	private void createChild(@NotNull SchemaNode parent, @NotNull Node node, @NotNull String label, List<String> path, @NotNull SerializationContext serializationContext) {
		if (serializationContext.nodeLookUp.containsKey(node.getUuid().toString())) {
			SchemaNode oldNode = serializationContext.nodeLookUp.get(node.getUuid().toString());
			logger.warn("Old Node: {}", oldNode);
			logger.warn("Replacing Node: {}", node);
			throw new RuntimeException("Ambiguity in node keys detected while converting model to CombinedModel for node id `" + oldNode.getUuid() + "`.");
		}
		SchemaNode schemaNode;
		if (node instanceof SetNode) {
			schemaNode = toCombinedModel((SetNode) node, label, path, serializationContext);
		} else if (node instanceof ObjectNode) {
			schemaNode = toCombinedModel((ObjectNode) node, label, path, serializationContext);
		} else if (node instanceof CompositeNode) {
			schemaNode = toCombinedModel((CompositeNode) node, label, path, serializationContext);
		} else if (node instanceof CollisionNode) {
			schemaNode = toCombinedModel((CollisionNode) node, label, path, serializationContext);
		} else if (node instanceof PrimitiveNode) {
			schemaNode = toCombinedModel((PrimitiveNode) node, label, path);
		} else {
			throw new IllegalArgumentException("Unsupported sub-type of " + Node.class + " found: " + node.getClass());
		}
		serializationContext.edges.add(new Edge(parent.getUuid(), schemaNode.getUuid()));
		serializationContext.nodeLookUp.put(node.getUuid().toString(), schemaNode);
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode toCombinedModel(@NotNull SetNode setNode, @NotNull String label, List<String> parentPath, @NotNull SerializationContext serializationContext) {

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (setNode.getPosition() != null) {
			xCoordinate = setNode.getPosition().getXCoordinate();
			yCoordinate = setNode.getPosition().getYCoordinate();
		}
		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(label);

		de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode root =
				new de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode(
						UUID.randomUUID().toString(),
						label,
						nodePath,
						xCoordinate,
						yCoordinate,
						setNode.isValid(), true, false);

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does gut a new instance

		for (SetNode.Child child : setNode.getChildren()) {
			createChild(root, child.getNode(), toLabel(child.getSelector()), nodePath, serializationContext);
		}

		return root;
	}

	@NotNull
	private String toLabel(@NotNull Selector selector) {
		if (selector instanceof WildcardSelector) {
			return "*";
		} else if (selector instanceof IndexSelector) {
			return StringUtilities.integerToOrdinal(((IndexSelector) selector).getElementIndex(), true);
		} else if (selector instanceof PatternSelector) {
			return selector.serialize();
		} else {
			logger.warn("Unknown subtype of {} found: {}", Selector.class, selector.getClass());
			return selector.serialize();
		}
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode toCombinedModel(@NotNull ObjectNode objectNode, @NotNull String label, List<String> parentPath, @NotNull SerializationContext serializationContext) {

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (objectNode.getPosition() != null) {
			xCoordinate = objectNode.getPosition().getXCoordinate();
			yCoordinate = objectNode.getPosition().getYCoordinate();
		}

		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(label);

		de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode root =
				new de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode(
						label, nodePath, xCoordinate, yCoordinate, objectNode.isValid());

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does gut a new instance

		for (Map.Entry<String, Node> entry : objectNode.getChildren().entrySet()) {
			createChild(root, entry.getValue(), entry.getKey(), nodePath, serializationContext);
		}

		return root;
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode toCombinedModel(@NotNull CompositeNode compositeNode, @NotNull String label, List<String> parentPath, @NotNull SerializationContext serializationContext) {

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (compositeNode.getPosition() != null) {
			xCoordinate = compositeNode.getPosition().getXCoordinate();
			yCoordinate = compositeNode.getPosition().getYCoordinate();
		}

		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(label);

		de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode root = new de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode(
				UUID.randomUUID().toString(),
				label,
				nodePath,
				xCoordinate,
				yCoordinate,
				compositeNode.isValid(),
				compositeNode.getExamples(),
				compositeNode.getCleansingPattern(),
				compositeNode.getSplitter().stream().map(this::splitterToDTO).collect(Collectors.toList())
		);

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does gut a new instance

		List<PrimitiveNode> components = compositeNode.getComponents();
		for (int i = 0; i < components.size(); i++) {
			createChild(root, components.get(i), StringUtilities.integerToOrdinal(i, true), nodePath, serializationContext);
		}

		final List<@NotNull SchemaNode> componentDTOs = serializationContext.edges.stream()
				.filter(edgeDTO -> edgeDTO.getFromId().equals(compositeNode.getUuid().toString()))
				.map(Edge::getToId)
				.map(serializationContext.nodeLookUp::get)
				.collect(Collectors.toList());

		for (int i = 0; i < componentDTOs.size() - 1; i++) {
			SchemaNode fromComponentDTO = componentDTOs.get(i);
			SchemaNode toComponentDTO = componentDTOs.get(i + 1);
			Edge edge = new Edge(fromComponentDTO.getUuid(), toComponentDTO.getUuid());
			serializationContext.edges.add(edge);
		}

		return root;
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema toCombinedModel(@NotNull CollisionNode collisionNode, @NotNull String label, List<String> parentPath, @NotNull SerializationContext serializationContext) {

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (collisionNode.getPosition() != null) {
			xCoordinate = collisionNode.getPosition().getXCoordinate();
			yCoordinate = collisionNode.getPosition().getYCoordinate();
		}

		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(label);

		de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema root = new de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema(
				UUID.randomUUID().toString(),
				COLLISION_ELEMENT_LABEL,
				nodePath,
				xCoordinate,
				yCoordinate,
				collisionNode.isValid()
		);

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does gut a new instance

		for (Node node : Arrays.asList(collisionNode.getPrimitiveNode(), collisionNode.getSetNode(), collisionNode.getSetNode())) {
			createChild(root, node, label, nodePath, serializationContext);
		}

		return root;
	}

	@Nullable
	private Splitting splitterToDTO(@Nullable Splitter splitter) {
		if (null == splitter) {
			return null;
		}
		return new Splitting(splitter.getPattern());
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode toCombinedModel(
			@NotNull PrimitiveNode primitiveNode,
			@NotNull String label,
			List<String> parentPath
	) {

		//transform position to coordinates
		Double xCoordinate = null;
		Double yCoordinate = null;
		if (primitiveNode.getPosition() != null) {
			xCoordinate = primitiveNode.getPosition().getXCoordinate();
			yCoordinate = primitiveNode.getPosition().getYCoordinate();
		}

		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(label);

		return new de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode(
				UUID.randomUUID().toString(),
				label,
				nodePath,
				xCoordinate,
				yCoordinate,
				primitiveNode.isValid(),
				DataType.valueOf(primitiveNode.getDataType().identifier),
				primitiveNode.getExamples(),
				primitiveNode.getCleansingPattern(),true, false
		);
	}

	/**
	 * Contains the current state of converted elements.
	 */
	private static class SerializationContext {
		private final HashMap<@NotNull String, @NotNull SchemaNode> nodeLookUp;
		private final ArrayList<@NotNull Edge> edges;

		public SerializationContext(
		) {
			this.nodeLookUp = new HashMap<>();
			this.edges = new ArrayList<>();
		}
	}
}
