package de.buw.tmdt.plasma.services.dms.core.converter;

import de.buw.tmdt.plasma.datamodel.*;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Splitting;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.dms.core.model.Position;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceSchema;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.Relation;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.*;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.members.*;
import de.buw.tmdt.plasma.services.dms.core.operations.Operation;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.repository.EntityTypeRepository;
import de.buw.tmdt.plasma.services.dms.core.repository.RelationConceptPropertyRepository;
import de.buw.tmdt.plasma.utilities.misc.Pair;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts between the shared {@link CombinedModel}
 * and the local {@link DataSourceSchema}
 */
@Service
public class CombinedModelConverter {

	private static final Logger logger = LoggerFactory.getLogger(CombinedModelConverter.class);
	private static final String ROOT_ELEMENT_LABEL = "ROOT";
	private static final String COLLISION_ELEMENT_LABEL = "COLLISION";

	private final OperationLookUp operationLookUp;
	private final SchemaOperationDTOConverter schemaOperationDTOConverter;
	private final RelationConceptPropertyRepository relationConceptPropertyRepository;
	private final EntityTypeRepository entityTypeRepository;

	@Autowired
	public CombinedModelConverter(
			@NotNull OperationLookUp operationLookUp,
			@NotNull SchemaOperationDTOConverter schemaOperationDTOConverter,
			@NotNull RelationConceptPropertyRepository relationConceptPropertyRepository,
			@NotNull EntityTypeRepository entityTypeRepository) {
		this.operationLookUp = operationLookUp;
		this.schemaOperationDTOConverter = schemaOperationDTOConverter;
		this.relationConceptPropertyRepository = relationConceptPropertyRepository;
		this.entityTypeRepository = entityTypeRepository;
	}

	/* Coversion methods  DMS model ===>  CombinedModel  */

	@NotNull
	public CombinedModel toCombinedModel(@NotNull DataSourceSchema dataSourceSchema) {
		SerializationContext serializationContext = new SerializationContext(
				operationLookUp.getOperationHandles(dataSourceSchema.getSyntaxModel())
		);
		SemanticModel sharedSemanticModel = toCombinedModel(dataSourceSchema.getSemanticModel(), serializationContext);

		Pair<SyntaxModel, List<PrimitiveEntityTypeMapping>> syntaxModel = toCombinedModel(dataSourceSchema.getSyntaxModel(), serializationContext);

		SemanticModel modifiedSemanticModel = new SemanticModel(
				dataSourceSchema.getSemanticModel().getUuid(),
				new ArrayList<>(serializationContext.entityTypes.values()),
				sharedSemanticModel.getEdges()
		);

		return new CombinedModel(
				dataSourceSchema.getSyntaxModel().getUuid().toString(),
				syntaxModel.getLeft(),
				modifiedSemanticModel,
				syntaxModel.getRight(),
				// re-enitialize, as we have reloaded from db rendering all node ids invalid
				// thus rendering all recommendations invalid
				new ArrayList<>(),
				dataSourceSchema.isFinalized()
		);
	}

	/* Syntax model conversion */

	@NotNull
	private Pair<SyntaxModel, List<PrimitiveEntityTypeMapping>> toCombinedModel(@NotNull Node root, @NotNull SerializationContext serializationContext) {
		SchemaNode syntaxRoot;
		if (root instanceof SetNode) {
			syntaxRoot = toCombinedModel((SetNode) root, ROOT_ELEMENT_LABEL, serializationContext);
		} else if (root instanceof ObjectNode) {
			syntaxRoot = toCombinedModel((ObjectNode) root, ROOT_ELEMENT_LABEL, serializationContext);
		} else {
			throw new IllegalArgumentException(root.toString());
		}
		serializationContext.nodeLookUp.put(root.getUuid().toString(), syntaxRoot);
		return new Pair<>(
				new SyntaxModel(
						syntaxRoot.getUuid(),
						new ArrayList<>(serializationContext.nodeLookUp.values()),
						serializationContext.edges
				), serializationContext.syntaxToSemanticModelEdges
		);
	}

	private void createChild(@NotNull SchemaNode parent, @NotNull Node node, @NotNull String label, @NotNull SerializationContext serializationContext) {
		if (serializationContext.nodeLookUp.containsKey(node.getUuid().toString())) {
			SchemaNode oldDTO = serializationContext.nodeLookUp.get(node.getUuid().toString());
			logger.warn("Old Node: {}", oldDTO);
			logger.warn("Replacing Node: {}", node);
			throw new RuntimeException("Ambiguity in node keys detected while converting model to dto for node id `" + oldDTO.getUuid() + "`.");
		}

		SchemaNode newDTO;
		if (node instanceof SetNode) {
			newDTO = toCombinedModel((SetNode) node, label, serializationContext);
		} else if (node instanceof ObjectNode) {
			newDTO = toCombinedModel((ObjectNode) node, label, serializationContext);
		} else if (node instanceof CompositeNode) {
			newDTO = toCombinedModel((CompositeNode) node, label, serializationContext);
		} else if (node instanceof CollisionNode) {
			newDTO = toCombinedModel((CollisionNode) node, label, serializationContext);
		} else if (node instanceof PrimitiveNode) {
			newDTO = toCombinedModel(parent, (PrimitiveNode) node, label, serializationContext);
		} else {
			throw new IllegalArgumentException("Unsupported sub-type of " + Node.class + " found: " + node.getClass());
		}
		serializationContext.edges.add(new Edge(parent.getUuid(), newDTO.getUuid(), ""));
		serializationContext.nodeLookUp.put(node.getUuid().toString(), newDTO);
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode toCombinedModel(@NotNull SetNode setNode, @NotNull String label, @NotNull SerializationContext serializationContext) {
		List<SyntacticOperationDTO> syntacticOperationDTOs = getOperationDTOs(setNode, serializationContext);

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (setNode.getPosition() != null) {
			xCoordinate = setNode.getPosition().getXCoordinate();
			yCoordinate = setNode.getPosition().getYCoordinate();
		}

		de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode root =
				new de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode(
						UUID.randomUUID().toString(),
						label,
						xCoordinate,
						yCoordinate,
						syntacticOperationDTOs,
						setNode.isValid());

		for (SetNode.Child child : setNode.getChildren()) {
			createChild(root, child.getNode(), toLabel(child.getSelector()), serializationContext);
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
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode toCombinedModel(@NotNull ObjectNode objectNode, @NotNull String label, @NotNull SerializationContext serializationContext) {
		List<SyntacticOperationDTO> syntacticOperationDTOs = getOperationDTOs(objectNode, serializationContext);

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (objectNode.getPosition() != null) {
			xCoordinate = objectNode.getPosition().getXCoordinate();
			yCoordinate = objectNode.getPosition().getYCoordinate();
		}

		de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode root =
				new de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode(
						UUID.randomUUID().toString(), label, xCoordinate, yCoordinate, syntacticOperationDTOs, objectNode.isValid());

		for (Map.Entry<String, Node> entry : objectNode.getChildren().entrySet()) {
			createChild(root, entry.getValue(), entry.getKey(), serializationContext);
		}

		return root;
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode toCombinedModel(@NotNull CompositeNode compositeNode, @NotNull String label, @NotNull SerializationContext serializationContext) {
		List<SyntacticOperationDTO> syntacticOperationDTOs = getOperationDTOs(compositeNode, serializationContext);

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (compositeNode.getPosition() != null) {
			xCoordinate = compositeNode.getPosition().getXCoordinate();
			yCoordinate = compositeNode.getPosition().getYCoordinate();
		}

		de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode root = new de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode(
				UUID.randomUUID().toString(),
				label,
				xCoordinate,
				yCoordinate,
				syntacticOperationDTOs,
				compositeNode.isValid(),
				compositeNode.getCleansingPattern(),
				compositeNode.getSplitter().stream().map(this::splitterToDTO).collect(Collectors.toList())
		);

		List<PrimitiveNode> components = compositeNode.getComponents();
		List<Splitter> splitter = compositeNode.getSplitter();
		for (int i = 0; i < components.size(); i++) {
			createChild(root, components.get(i), StringUtilities.integerToOrdinal(i, true), serializationContext);
		}

		final List<@NotNull SchemaNode> componentDTOs = serializationContext.edges.stream()
				.filter(edgeDTO -> edgeDTO.getFrom().equals(compositeNode.getUuid().toString()))
				.map(Edge::getTo)
				.map(serializationContext.nodeLookUp::get)
				.collect(Collectors.toList());

		for (int i = 0; i < componentDTOs.size() - 1; i++) {
			SchemaNode fromComponentDTO = componentDTOs.get(i);
			SchemaNode toComponentDTO = componentDTOs.get(i + 1);
			serializationContext.edges.add(new Edge(fromComponentDTO.getUuid(), toComponentDTO.getUuid(), splitter.get(i).getPattern()));
		}

		return root;
	}

	@NotNull
	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Explicit check that value is not null")
	private de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema toCombinedModel(@NotNull CollisionNode collisionNode, @NotNull String label, @NotNull SerializationContext serializationContext) {
		List<SyntacticOperationDTO> syntacticOperationDTOs = getOperationDTOs(collisionNode, serializationContext);

		Double xCoordinate = null;
		Double yCoordinate = null;
		if (collisionNode.getPosition() != null) {
			xCoordinate = collisionNode.getPosition().getXCoordinate();
			yCoordinate = collisionNode.getPosition().getYCoordinate();
		}

		de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema root = new de.buw.tmdt.plasma.datamodel.syntaxmodel.CollisionSchema(
				UUID.randomUUID().toString(),
				COLLISION_ELEMENT_LABEL,
				xCoordinate,
				yCoordinate,
				syntacticOperationDTOs,
				collisionNode.isValid()
		);

		for (Node node : Arrays.asList(collisionNode.getPrimitiveNode(), collisionNode.getSetNode(), collisionNode.getSetNode())) {
			createChild(root, node, label, serializationContext);
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
			@NotNull SchemaNode parent,
			@NotNull PrimitiveNode primitiveNode,
			@NotNull String label,
			@NotNull SerializationContext serializationContext
	) {
		// todo move operations injection to external call
		//inject operations
		List<SyntacticOperationDTO> syntacticOperationDTOs = getOperationDTOs(primitiveNode, serializationContext);
		syntacticOperationDTOs.sort(Comparator.comparing(SyntacticOperationDTO::getName));

		//transform position to coordinates
		Double xCoordinate = null;
		Double yCoordinate = null;
		if (primitiveNode.getPosition() != null) {
			xCoordinate = primitiveNode.getPosition().getXCoordinate();
			yCoordinate = primitiveNode.getPosition().getYCoordinate();
		}

		final de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode primitiveDTO = new de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode(
				UUID.randomUUID().toString(),
				label,
				xCoordinate,
				yCoordinate,
				syntacticOperationDTOs,
				primitiveNode.isValid(),
				primitiveNode.getDataType().getSerialization(),
				primitiveNode.getExamples(),
				primitiveNode.getCleansingPattern()
		);

		if (primitiveNode.getEntityType() != null) {
			EntityType unmappedEntityType = serializationContext.entityTypes.entrySet().stream()
					.filter(entityType -> primitiveNode.getEntityType().getId().equals(entityType.getKey()))
					.map(Map.Entry::getValue)
					.findFirst()
					.orElseThrow(() -> new RuntimeException("Missing entity type in semantic model which was referenced by: " + primitiveNode));
			unmappedEntityType.setOriginalLabel(primitiveDTO.getLabel());
			unmappedEntityType.setSyntaxNodeUuid(primitiveDTO.getUuid());

			serializationContext.syntaxToSemanticModelEdges.add(new PrimitiveEntityTypeMapping(
					parent.getUuid(),
					unmappedEntityType.getUuid(),
					primitiveDTO.getUuid()
			));
		}
		return primitiveDTO;
	}

	/* Semantic model conversion */

	@NotNull
	public SemanticModel toCombinedModel(@NotNull de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.SemanticModel semanticModel, SerializationContext serializationContext) {
		List<de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType> entityTypeLookUp = semanticModel.getEntityTypes().stream()
				.map(entityType -> toCombinedModel(entityType, serializationContext))
				.collect(de.buw.tmdt.plasma.utilities.collections.Collectors.getSerializableListCollector());

		List<de.buw.tmdt.plasma.datamodel.semanticmodel.Relation> relations = semanticModel.getRelations().stream()
				.map(relation -> toCombinedModel(relation, serializationContext))
				.collect(de.buw.tmdt.plasma.utilities.collections.Collectors.getSerializableListCollector());

		return new de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel(
				semanticModel.getUuid(),
				new ArrayList<>(entityTypeLookUp),
				new ArrayList<>(relations)
		);
	}

	/**
	 * Converts an {@link de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType} to a
	 * {@link EntityType} to be used in a {@link CombinedModel}
	 *
	 * @param entityType The DMS internal entity type to convert
	 * @return The converted ET for the {@link CombinedModel}
	 */
	@NotNull
	private de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType toCombinedModel(@NotNull de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType entityType, SerializationContext serializationContext) {
		Double xCoordinate = null;
		Double yCoordinate = null;
		if (entityType.getPosition() != null) {
			xCoordinate = entityType.getPosition().getXCoordinate();
			yCoordinate = entityType.getPosition().getYCoordinate();
		}

		de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept entityConcept = toCombinedModel(entityType.getEntityConcept());

		EntityType sharedEntityType = new EntityType(
				UUID.randomUUID().toString(),
				entityType.getUuid(),
				entityType.getLabel(),
				null, // this will be inserted later when the syntax model is converted as the links are stored there
				entityType.getOriginalLabel(),
				entityType.getDescription(),
				entityConcept,
				xCoordinate,
				yCoordinate
		);
		// put the new entity type in the serialization lookup map
		serializationContext.entityTypes.put(entityType.getId(), sharedEntityType);
		return sharedEntityType;
	}

	/**
	 * Converts an {@link de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept} to a
	 * {@link EntityConcept} to be used in a {@link CombinedModel}
	 *
	 * @param entityConcept The DMS internal type to convert
	 * @return The converted EC for the {@link CombinedModel}
	 */
	@NotNull
	public de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept toCombinedModel(@NotNull EntityConcept entityConcept) {
		return new de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept(
				UUID.randomUUID().toString(),
				entityConcept.getName(),
				entityConcept.getUuid(),
				entityConcept.getDescription(),
				entityConcept.getSourceURI()
		);
	}

	/**
	 * Converts an {@link de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.Relation} to a
	 * {@link Relation} to be used in a {@link CombinedModel}
	 *
	 * @param relation The DMS internal type to convert
	 * @return The converted EC for the {@link CombinedModel}
	 */
	@NotNull
	private de.buw.tmdt.plasma.datamodel.semanticmodel.Relation toCombinedModel(
			@NotNull Relation relation,
			@NotNull SerializationContext context) {

		String fromUuid = context.entityTypes.entrySet().stream()
				.filter(longEntityTypeEntry -> longEntityTypeEntry.getKey().equals(relation.getFrom().getId()))
				.map(Map.Entry::getValue)
				.map(CombinedModelElement::getUuid)
				.findFirst().orElseThrow(() -> new RuntimeException("Missing entity type in semantic model which was referenced in 'from' by: " + relation));
		String toUuid = context.entityTypes.entrySet().stream()
				.filter(longEntityTypeEntry -> longEntityTypeEntry.getKey().equals(relation.getTo().getId()))
				.map(Map.Entry::getValue)
				.map(CombinedModelElement::getUuid)
				.findFirst().orElseThrow(() -> new RuntimeException("Missing entity type in semantic model which was referenced in 'to' by: " + relation));

		return new de.buw.tmdt.plasma.datamodel.semanticmodel.Relation(
				fromUuid,
				toUuid,
				toCombinedModel(relation.getRelationConcept())
		);
	}

	/**
	 * Converts an {@link de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept} to a
	 * {@link RelationConcept} to be used in a {@link CombinedModel}
	 *
	 * @param relationConcept The DMS internal type to convert
	 * @return The converted EC for the {@link CombinedModel}
	 */
	@NotNull
	public de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept toCombinedModel(@NotNull RelationConcept relationConcept) {
		return new de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept(
				UUID.randomUUID().toString(),
				relationConcept.getName(),
				relationConcept.getUuid(),
				relationConcept.getDescription(),
				relationConcept.getSourceURI(),
				relationConcept.getProperties().stream().map(RelationConcept.Property::getName).collect(de.buw.tmdt.plasma.utilities.collections.Collectors.getSerializableSetCollector())
		);
	}

	/* Conversion methods CombinedModel  ===>  DMS model   */

	/**
	 * Converts the given {@link CombinedModel} to a {@link DataSourceSchema}
	 *
	 * @param combinedModel The {@link CombinedModel} to convert
	 * @return The converted {@link DataSourceSchema}
	 */
	public @NotNull DataSourceSchema fromCombinedModel(@NotNull DataSourceModel dataSourceModel, @NotNull CombinedModel combinedModel) throws CombinedModelIntegrityException {

		Map<String, de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType> entityTypes =
				combinedModel.getSemanticModel().getNodes().stream()
						.collect(Collectors.toMap(EntityType::getUuid, this::fromCombinedModel));

		List<Relation> relations = combinedModel.getSemanticModel().getEdges().stream()
				.map(relationDTO -> fromCombinedModel(relationDTO, entityTypes))
				.collect(Collectors.toList());

		de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.SemanticModel semanticModel =
				new de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.SemanticModel(
						null,
						"",
						"",
						new ArrayList<>(entityTypes.values()),
						relations,
						null);

		Node root = fromCombinedModel(combinedModel.getSyntaxModel());

		return new DataSourceSchema(dataSourceModel, root, semanticModel, new ArrayList<>(), new ArrayList<>());
	}

	private Node fromCombinedModel(SyntaxModel syntaxModel) throws CombinedModelIntegrityException {
		Map<String, SchemaNode> nodes = syntaxModel.getNodes().stream().collect(Collectors.toMap(CombinedModelElement::getUuid, Function.identity()));
		List<Edge> edges = new ArrayList<>(syntaxModel.getEdges());
		SchemaNode root = nodes.get(syntaxModel.getRoot());
		if (root == null) {
			throw new CombinedModelIntegrityException("Root node " + syntaxModel.getRoot() + " is not present.");
		}
		convertSyntaxNode(root, nodes, edges);
		// TODO
		return null;
	}

	private @NotNull Node convertSyntaxNode(SchemaNode schemaNode, Map<String, SchemaNode> nodes, List<Edge> edges) {
		Node node = null;

		// get outgoing edges
		List<Edge> outgoing = edges.stream()
				.filter(edge -> edge.getFrom().equals(schemaNode.getUuid()))
				.collect(Collectors.toList());

		// recursively call for all child nodes
		Set<Node> children = outgoing.stream().map(edge -> nodes.get(edge.getFrom()))
				.map(targetNode -> convertSyntaxNode(targetNode, nodes, edges))
				.collect(Collectors.toSet());

		// convert the node
		if (schemaNode instanceof de.buw.tmdt.plasma.datamodel.syntaxmodel.SetNode) {
			SetNode setNode = new SetNode(children);
			Position p = getPosition(schemaNode);
			if (p != null) {
				setNode.setPosition(p);
			}
			node = setNode;
		} else if (schemaNode instanceof de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode) {
			// TODO
		} else if (schemaNode instanceof de.buw.tmdt.plasma.datamodel.syntaxmodel.CompositeNode) {
			// TODO
		} else if (schemaNode instanceof de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode) {
			// TODO
		}

		return node;
	}


	public @NotNull Relation fromCombinedModel(
			@NotNull de.buw.tmdt.plasma.datamodel.semanticmodel.Relation relation,
			@NotNull Map<String, de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType> entityTypes) {

		final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType from =
				entityTypes.get(relation.getFromId());
		if (from == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Entity type id in relation is invalid."
			);
		}

		final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType to =
				entityTypes.get(relation.getToId());
		if (to == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Entity type id in relation is invalid."
			);
		}
		return new Relation(relation.getEntityId(), from, to, fromCombinedModel(relation.getConcept()), relation.getDescription(), null);
	}

	public @NotNull Relation fromCombinedModel(@NotNull de.buw.tmdt.plasma.datamodel.semanticmodel.Relation relation) {
		final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType from = entityTypeRepository.findById(Long.valueOf(relation.getFromId()))
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Entity type id in relation is invalid."
				));

		final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType to = entityTypeRepository.findById(Long.valueOf(relation.getToId()))
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Entity type id in relation is invalid."
				));
		return new Relation(relation.getEntityId(), from, to, fromCombinedModel(relation.getConcept()), relation.getDescription(), null);
	}

	@NotNull
	public RelationConcept fromCombinedModel(@NotNull de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept concept) {
		Set<RelationConcept.Property> properties = new HashSet<>(relationConceptPropertyRepository.findAllByNameIn(concept.getProperties()));
		return new RelationConcept(
				concept.getEntityId(),
				concept.getLabel(),
				concept.getDescription(),
				concept.getSourceURI(),
				properties,
				null
		);
	}

	@NotNull
	public de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType fromCombinedModel(@NotNull de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType entityType) {
		return new de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType(
				entityType.getEntityId(),
				entityType.getLabel(),
				entityType.getOriginalLabel(),
				entityType.getDescription(),
				fromCombinedModel(entityType.getEntityConcept()),
				getPosition(entityType)
		);
	}

	@NotNull
	public EntityConcept fromCombinedModel(@NotNull de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept entityConcept) {
		return new EntityConcept(
				entityConcept.getEntityId(),
				entityConcept.getLabel(),
				entityConcept.getDescription(),
				entityConcept.getSourceURI(),
				null
		);

	}

	@NotNull
	private List<SyntacticOperationDTO> getOperationDTOs(@NotNull Node node, @NotNull SerializationContext serializationContext) {
		return serializationContext.parameterDefinitionLookup.getOrDefault(node, new HashSet<>()).stream()
				.map(handle -> new SyntacticOperationDTO(
						handle.getOperation().getName(),
						handle.getOperation().getLabel(),
						handle.getOperation().getDescription(),
						schemaOperationDTOConverter.toDTO(handle.getParameterDefinition())
				)).collect(Collectors.toList());
	}

	@Nullable
	private Position getPosition(@NotNull PositionedCombinedModelElement positioned) {
		Double xCoordinate = positioned.getXCoordinate();
		Double yCoordinate = positioned.getYCoordinate();
		if (xCoordinate != null && yCoordinate != null) {
			return new Position(xCoordinate, yCoordinate);
		}
		return null;
	}

	/**
	 * Contains the current state of coverted elements
	 */
	private static class SerializationContext {
		private final HashMap<@NotNull String, @NotNull SchemaNode> nodeLookUp;
		private final ArrayList<@NotNull Edge> edges;
		private final ArrayList<@NotNull PrimitiveEntityTypeMapping> syntaxToSemanticModelEdges;
		private final HashMap<Node, Set<Operation.Handle>> parameterDefinitionLookup;
		/**
		 * Maps database ids to their generated {@link EntityType}s.
		 * The database information is lost during the conversion.
		 */
		private final HashMap<@NotNull Long, @NotNull EntityType> entityTypes;

		public SerializationContext(
				HashMap<Node, Set<Operation.Handle>> parameterDefinitionLookup
		) {
			this.parameterDefinitionLookup = parameterDefinitionLookup;
			this.nodeLookUp = new HashMap<>();
			this.edges = new ArrayList<>();
			this.syntaxToSemanticModelEdges = new ArrayList<>();
			this.entityTypes = new HashMap<>();
		}
	}
}
