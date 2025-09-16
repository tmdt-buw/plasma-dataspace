package de.buw.tmdt.plasma.services.sms.schemaanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import static de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode.*;

import java.util.*;

/**
 * Analyzes the given {@link JsonNode} tree to create a representation as a {@link SyntaxModel}.
 */
@Service
public class StructureIdentificationService {
	private static final Logger logger = LoggerFactory.getLogger(StructureIdentificationService.class);
	private static final int EXAMPLE_VALUE_LENGTH = 255;
	private static final int NODE_LABEL_LENGTH = 254;

	/**
	 * Converts given tree to a {@link SyntaxModel}.
	 *
	 * @param root The root element of the json tree
	 * @return The resulting {@link SyntaxModel}
	 */
	@NotNull
	public SyntaxModel convertRootNode(@NotNull JsonNode root) {
		@NotNull SerializationContext serializationContext = new SerializationContext();
		SyntaxNode syntaxRoot;
		List<String> path = new ArrayList<>();

		if (root.isArray()) {
			syntaxRoot = convertArrayNode((ArrayNode) root, "ROOT", ROOT_PATH_TOKEN, path, serializationContext);
		} else if (root.isObject()) {
			syntaxRoot = convertObjectNode((com.fasterxml.jackson.databind.node.ObjectNode) root, "ROOT", ROOT_PATH_TOKEN, path, serializationContext);
		} else {
			throw new IllegalArgumentException(root.toString());
		}
		SyntaxModel syntaxModel = new SyntaxModel(
				syntaxRoot.getUuid(),
				new ArrayList<>(serializationContext.nodeLookUp.values()),
				serializationContext.edges
		);
		syntaxModel.validate();
		return syntaxModel;
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode convertArrayNode(@NotNull ArrayNode arrayNode, @NotNull String label, String pathToken, List<String> parentPath, @NotNull SerializationContext serializationContext) {

		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(pathToken);

		SetNode root =
				new de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode(
						UUID.randomUUID().toString(),
						label,
						nodePath,
						null, null,
						true, false);

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does get a new instance

		if(!arrayNode.isEmpty()) {
			createArrayChild(root, arrayNode.get(0), nodePath, serializationContext);
		}
		return root;
	}

	private void createArrayChild(@NotNull SyntaxNode parentNode, @NotNull JsonNode child, List<String> nodePath, @NotNull SerializationContext serializationContext) {
		SyntaxNode syntaxNode;

		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does get a new instance
		if (child.isArray()) {
			syntaxNode = convertArrayNode((ArrayNode) child, ARRAY_LABEL, ARRAY_PATH_TOKEN, nodePath, serializationContext);
		} else if (child.isObject()) {
			syntaxNode = convertObjectNode((com.fasterxml.jackson.databind.node.ObjectNode) child, OBJECT_LABEL, ARRAY_PATH_TOKEN, nodePath, serializationContext);
		} else if (child.isValueNode()) {
			syntaxNode = convertValueNode((ValueNode) child, VALUE_LABEL, ARRAY_PATH_TOKEN, nodePath);
		} else {
			throw new IllegalArgumentException("Unsupported sub-type of " + JsonNode.class + " found: " + child.getClass());
		}
		serializationContext.nodeLookUp.put(syntaxNode.getUuid(), syntaxNode);
		serializationContext.edges.add(new Edge(parentNode.getUuid(), syntaxNode.getUuid()));
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode convertObjectNode(@NotNull com.fasterxml.jackson.databind.node.ObjectNode objectNode, @NotNull String label, @NotNull String pathToken, List<String> parentPath, @NotNull SerializationContext serializationContext) {
		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(pathToken);
		de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode object = new de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode(label, nodePath, null, null);

		serializationContext.nodeLookUp.put(object.getUuid(), object);
		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does gut a new instance

		for (Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> entry = it.next();
			String nodeLabel = entry.getKey().substring(0, Math.min(entry.getKey().length(), NODE_LABEL_LENGTH));
			createObjectChild(object, entry.getValue(), nodeLabel, entry.getKey(), nodePath, serializationContext);
		}

		return object;
	}

	private void createObjectChild(@NotNull de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode parentObjectNode,
								   @NotNull JsonNode child,
								   String label,
								   String pathToken,
								   List<String> nodePath,
								   @NotNull SerializationContext serializationContext) {
		SyntaxNode syntaxNode;
		nodePath = new ArrayList<>(nodePath); // copy list again to ensure child does get a new instance
		if (child.isArray()) {
			syntaxNode = convertArrayNode((ArrayNode) child, label, pathToken, nodePath, serializationContext);
		} else if (child.isObject()) {
			syntaxNode = convertObjectNode((com.fasterxml.jackson.databind.node.ObjectNode) child, label, pathToken, nodePath, serializationContext);
		} else if (child.isValueNode()) {
			syntaxNode = convertValueNode((ValueNode) child, label, pathToken, nodePath);
		} else {
			throw new IllegalArgumentException("Unsupported sub-type of " + JsonNode.class + " found: " + child.getClass());
		}
		serializationContext.nodeLookUp.put(syntaxNode.getUuid(), syntaxNode);
		serializationContext.edges.add(new Edge(parentObjectNode.getUuid(), syntaxNode.getUuid()));
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private PrimitiveNode convertValueNode(
			@NotNull ValueNode valueNode,
			@NotNull String label,
			String pathToken,
			List<String> parentPath
	) {
		List<String> nodePath = new ArrayList<>(parentPath);
		nodePath.add(pathToken);

		String exampleValue = valueNode.asText();
		String truncatedExampleValue = exampleValue.substring(0, Math.min(exampleValue.length(), EXAMPLE_VALUE_LENGTH));
		List<String> exampleValues = new ArrayList<>();
		exampleValues.add(truncatedExampleValue);

		return new PrimitiveNode(
				UUID.randomUUID().toString(),
				label,
				nodePath,
				null,
				null,
				DataType.Unknown,
				exampleValues,
				true,
				false
		);
	}

	/**
	 * Contains the current state of converted elements.
	 */
	private static class SerializationContext {
		private final HashMap<@NotNull String, @NotNull SyntaxNode> nodeLookUp;
		private final ArrayList<@NotNull Edge> edges;

		public SerializationContext(
		) {
			this.nodeLookUp = new HashMap<>();
			this.edges = new ArrayList<>();
		}
	}

}
