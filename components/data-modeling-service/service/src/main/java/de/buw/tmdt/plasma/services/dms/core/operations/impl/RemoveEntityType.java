package de.buw.tmdt.plasma.services.dms.core.operations.impl;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.ParameterDefinition;
import de.buw.tmdt.plasma.datamodel.modification.operation.Type;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.services.dms.core.operations.Operation;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.operations.exceptions.ParameterParsingException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static de.buw.tmdt.plasma.services.dms.core.operations.Operation.Strings.PRIMITIVE_NODE_IDENTIFIER;

/**
 * This operation removes an EntityType from a node referred by its nodeId. The operation is attached to all
 * PrimitiveNodes.
 */
@Service
public class RemoveEntityType extends Operation<PrimitiveNode> {

	private static final String OPERATION_NAME = "RemoveEntityType";
	private static final String NODE_ID_PARAMETER_NAME = "NodeId";

	private static final ParameterDefinition<String, String> PARAMETER_PROTOTYPE =
			new ParameterDefinition<>(
					Type.SYNTAX_NODE_ID,
					NODE_ID_PARAMETER_NAME,
					PRIMITIVE_NODE_IDENTIFIER,
					"The identifier of the primitive node of which the entity type shall be disconnected.",
					1,
					1,
					true
			);

	@Autowired
	protected RemoveEntityType(OperationLookUp operationLookUp) {
		super(
				OPERATION_NAME,
				PARAMETER_PROTOTYPE,
				"Disconnect entity type",
				"Removes the connection of the selected data structure node to it's entity type.",
				operationLookUp
		);
	}

	@Override
	public PrimitiveNode parseParameterDefinition(CombinedModel model, ParameterDefinition<?, ?> parameterDefinition) throws ParameterParsingException {
		final String nodeId;
		validateParameterDefinition(parameterDefinition);

		ParameterDefinition<String, String> uuidParameterDefinition = Type.SYNTAX_NODE_ID.castParameterDefinition(parameterDefinition);
		nodeId = getValueAs(uuidParameterDefinition, Type.SYNTAX_NODE_ID, 0, false);

		SchemaNode node = model.getSyntaxModel().getNode(nodeId);
		if (!(node instanceof PrimitiveNode)) {
			throw new ParameterParsingException("Did not find node with id `" + nodeId + "` in model.");
		}

		return (PrimitiveNode) node;
	}

	@Override
	protected CombinedModel execute(CombinedModel model, PrimitiveNode object) {
		// in the CombinedModel, the association is stored in the semantic model's EntityType
		EntityType entityType = model.getSemanticModel().getNodes().stream()
				.filter(et -> et.getSyntaxNodeUuid().equals(object.getUuid()))
				.findFirst().orElse(null);

		if (entityType != null) {
			EntityType modifiedEntityType = entityType.copy();
			modifiedEntityType.setSyntaxNodeUuid(null);
			modifiedEntityType.setOriginalLabel(null);
			DeltaModification modification = new DeltaModification("local_operation", Set.of(modifiedEntityType), null, null, null);
			model.apply(modification); // this will also delete the explicit mapping
		}
		return model;
	}

	@Override
	protected Handle getHandleOnNode(SchemaNode node) {
		ParameterDefinition<?, ?> parameterDefinitionClone = getParameterPrototype();
		parameterDefinitionClone.replaceValue(NODE_ID_PARAMETER_NAME, node.getUuid());
		return new Handle(this, parameterDefinitionClone);
	}

	@Override
	public Map<SchemaNode, Set<Handle>> generateHandlesForApplicableNodes(@NotNull CombinedModel model) {
		Map<SchemaNode, Set<Handle>> result = new HashMap<>();
		// get all uuids of mapped primitive nodes
		List<String> mappedPrimitiveNodeUuids = model.getSemanticModel().getNodes().stream()
				.map(EntityType::getSyntaxNodeUuid)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		// get the matching nodes
		mappedPrimitiveNodeUuids.stream()
				.map(s -> model.getSyntaxModel().getNode(s))
				.forEach(schemaNode -> result.computeIfAbsent(schemaNode, ignored -> new HashSet<>()).add(getHandleOnNode(schemaNode)));
		return result;
	}
}
