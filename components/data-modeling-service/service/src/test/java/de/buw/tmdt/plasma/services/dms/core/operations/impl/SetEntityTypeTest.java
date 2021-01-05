package de.buw.tmdt.plasma.services.dms.core.operations.impl;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.modification.operation.ParameterDefinition;
import de.buw.tmdt.plasma.datamodel.modification.operation.Type;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.operations.exceptions.ParameterParsingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetEntityTypeTest {

	@Mock
	private OperationLookUp lookUpMock;

	private CombinedModel combinedModel;

	private EntityType entityType;
	private SetEntityType testee;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);

		entityType = new EntityType(
				"label",
				"description",
				new EntityConcept(UUID.randomUUID().toString(), "ECName", "ECDescription", "ECSourceURI", null)
		);

		testee = new SetEntityType(lookUpMock);
	}

	@Test
	void testInvoke() throws ParameterParsingException {
		SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

		SchemaNode node = new PrimitiveNode(
				"label",
				DataType.UNKNOWN.identifier
		);
		Edge edge = new Edge(root.getUuid(), node.getUuid());

		SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), new ArrayList<>(Arrays.asList(root, node)), new ArrayList<>(Collections.singletonList(edge)));
		SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, semanticModel);

		testee.invoke(combinedModel, setParameters(node.getUuid(), entityType.getUuid()));

		entityType = combinedModel.getSemanticModel().getEntityType(entityType.getUuid());
		node = combinedModel.getSyntaxModel().getNode(node.getUuid());

		assertEquals(node.getUuid(), entityType.getSyntaxNodeUuid());
		assertTrue(node instanceof PrimitiveNode);
		assertEquals(DataType.UNKNOWN.identifier, ((PrimitiveNode) node).getDataType());
	}

	// TODO move if functionality in operation is moved
	@Test
	void testInvokeStringDataType() throws ParameterParsingException {
		SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

		SchemaNode node = new PrimitiveNode(
				"label",
				DataType.UNKNOWN.identifier
		);
		Edge edge = new Edge(root.getUuid(), node.getUuid());

		((PrimitiveNode) node).setExamples(Arrays.asList("0", "myExampleString"));

		SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), new ArrayList<>(Arrays.asList(root, node)), new ArrayList<>(Collections.singletonList(edge)));
		SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, semanticModel);

		testee.invoke(combinedModel, setParameters(node.getUuid(), entityType.getUuid()));

		entityType = combinedModel.getSemanticModel().getEntityType(entityType.getUuid());
		node = combinedModel.getSyntaxModel().getNode(node.getUuid());

		assertEquals(node.getUuid(), entityType.getSyntaxNodeUuid());
		assertTrue(node instanceof PrimitiveNode);
		assertEquals(DataType.STRING.identifier, ((PrimitiveNode) node).getDataType());
	}

	@Test
	void testInvokeNumberDataType() throws ParameterParsingException {
		SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

		SchemaNode node = new PrimitiveNode(
				"label",
				DataType.UNKNOWN.identifier
		);
		Edge edge = new Edge(root.getUuid(), node.getUuid());

		((PrimitiveNode) node).setExamples(Arrays.asList("0", "42", "1", "0.0"));

		SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), new ArrayList<>(Arrays.asList(root, node)), new ArrayList<>(Collections.singletonList(edge)));
		SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, semanticModel);

		testee.invoke(combinedModel, setParameters(node.getUuid(), entityType.getUuid()));
		entityType = combinedModel.getSemanticModel().getEntityType(entityType.getUuid());
		node = combinedModel.getSyntaxModel().getNode(node.getUuid());

		assertEquals(node.getUuid(), entityType.getSyntaxNodeUuid());
		assertTrue(node instanceof PrimitiveNode);
		assertEquals(DataType.NUMBER.identifier, ((PrimitiveNode) node).getDataType());
	}

	@Test
	void testInvokeBooleanDataType() throws ParameterParsingException {
		SchemaNode root = new ObjectNode("root", new ArrayList<>(), true);

		SchemaNode node = new PrimitiveNode(
				"label",
				DataType.UNKNOWN.identifier
		);
		Edge edge = new Edge(root.getUuid(), node.getUuid());

		((PrimitiveNode) node).setExamples(Arrays.asList("0", "1", "t", "f", "true", "false"));

		// actually create both models as they contain some useful methods
		SyntaxModel syntaxModel = new SyntaxModel(root.getUuid(), new ArrayList<>(Arrays.asList(root, node)), new ArrayList<>(Collections.singletonList(edge)));
		SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, semanticModel);

		testee.invoke(combinedModel, setParameters(node.getUuid(), entityType.getUuid()));

		entityType = combinedModel.getSemanticModel().getEntityType(entityType.getUuid());
		node = combinedModel.getSyntaxModel().getNode(node.getUuid());

		assertEquals(node.getUuid(), entityType.getSyntaxNodeUuid());
		assertTrue(node instanceof PrimitiveNode);
		assertEquals(DataType.BOOLEAN.identifier, ((PrimitiveNode) node).getDataType());
	}

	@Test
	void wrongNodeId() {
		SchemaNode node = new PrimitiveNode("node1", DataType.UNKNOWN.identifier);

		SyntaxModel syntaxModel = new SyntaxModel(node.getUuid(), new ArrayList<>(Collections.singletonList(node)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, null);

		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, setParameters(node.getUuid(), null));
		});


		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, setParameters(null, null));
		});

		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, setParameters(UUID.randomUUID().toString(), null));
		});
	}

	@Test
	void wrongNodeType() {
		SchemaNode node = new SetNode(new ArrayList<>(), true);

		SyntaxModel syntaxModel = new SyntaxModel(node.getUuid(), new ArrayList<>(Collections.singletonList(node)), new ArrayList<>());

		combinedModel = new CombinedModel("", syntaxModel, null);

		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, setParameters(node.getUuid(), null));
		});
	}

	@Test
	void testRegistry() {
		verify(lookUpMock, times(1)).registerOperation(testee);
	}

	private ParameterDefinition setParameters(String nodeUuid, String entityTypeId) {
		return new ParameterDefinition<>(
				Type.COMPLEX,
				"",
				"",
				"",
				1,
				1,
				new ParameterDefinition<>(
						Type.SYNTAX_NODE_ID,
						SetEntityType.NODE_ID_PARAMETER_NAME,
						"",
						"",
						1,
						1,
						true,
						nodeUuid
				),
				new ParameterDefinition<>(
						Type.ENTITY_TYPE_ID,
						SetEntityType.ENTITY_TYPE_ID_PARAMETER_NAME,
						"",
						"",
						1,
						1,
						entityTypeId
				)
		);
	}
}