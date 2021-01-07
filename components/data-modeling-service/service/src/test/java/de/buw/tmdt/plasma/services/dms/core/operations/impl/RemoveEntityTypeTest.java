package de.buw.tmdt.plasma.services.dms.core.operations.impl;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.PrimitiveEntityTypeMapping;
import de.buw.tmdt.plasma.datamodel.modification.operation.ParameterDefinition;
import de.buw.tmdt.plasma.datamodel.modification.operation.Type;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.ObjectNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.operations.exceptions.ParameterParsingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveEntityTypeTest {

	@Mock
	private OperationLookUp lookUpMock;

	@Mock
	private PrimitiveNode primitiveNodeMock;

	private RemoveEntityType testee;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		testee = new RemoveEntityType(lookUpMock);
	}

	@Test
	void testInvoke() throws ParameterParsingException {
		String primitiveUuid = UUID.randomUUID().toString();

		when(primitiveNodeMock.getUuid()).thenReturn(primitiveUuid);

		EntityConcept concept = new EntityConcept(UUID.randomUUID().toString(), "ECName", "ECDescription", "ECSourceURI", "plasma:concept1");

		EntityType entityType = new EntityType("entityType", "", concept);
		entityType.setSyntaxNodeUuid(primitiveUuid);
		entityType.setOriginalLabel("TO BE REMOVED");

		SyntaxModel syntaxModel = new SyntaxModel(primitiveUuid, new ArrayList<>(Collections.singletonList(primitiveNodeMock)), new ArrayList<>());
		SemanticModel semanticModel = new SemanticModel(null, new ArrayList<>(Collections.singletonList(entityType)), new ArrayList<>());
		PrimitiveEntityTypeMapping mapping = new PrimitiveEntityTypeMapping(primitiveUuid, entityType.getUuid(), UUID.randomUUID().toString());

		List<PrimitiveEntityTypeMapping> mappings = new ArrayList<>(Collections.singletonList(mapping));

		CombinedModel combinedModel = new CombinedModel("blub", syntaxModel, semanticModel, mappings, new ArrayList<>(), false);

		testee.invoke(
				combinedModel,
				new ParameterDefinition<>(
						Type.SYNTAX_NODE_ID,
						"NodeId",
						"",
						"",
						1,
						1,
						true,
						primitiveUuid
				)
		);

		// validate results

		EntityType updatedEntityType = combinedModel.getSemanticModel().getEntityType(entityType.getUuid());

		assertNotNull(updatedEntityType);
		assertNull(updatedEntityType.getSyntaxNodeUuid());
		assertNull(updatedEntityType.getOriginalLabel());

		// check that the explicit mapping has been removed
		assertTrue(combinedModel.getPrimitiveEntityTypeMappings().isEmpty());
	}

	@Test
	void wrongNodeId() {
		String primitiveUuid = UUID.randomUUID().toString();
		when(primitiveNodeMock.getUuid()).thenReturn(primitiveUuid);


		SyntaxModel syntaxModel = new SyntaxModel(primitiveUuid, new ArrayList<>(Collections.singletonList(primitiveNodeMock)), new ArrayList<>());

		CombinedModel combinedModel = new CombinedModel("blub", syntaxModel, null);

		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, new ParameterDefinition<>(
					Type.SYNTAX_NODE_ID,
					"NodeId",
					"",
					"",
					1,
					1,
					true
			));
		});
	}

	@Test
	void wrongNodeType() {
		ObjectNode objectNode = new ObjectNode("objectNode", new ArrayList<>(), true);

		SyntaxModel syntaxModel = new SyntaxModel(objectNode.getUuid(), new ArrayList<>(Collections.singletonList(objectNode)), new ArrayList<>());

		CombinedModel combinedModel = new CombinedModel("blub", syntaxModel, null);

		Assertions.assertThrows(ParameterParsingException.class, () -> {
			testee.parseParameterDefinition(combinedModel, new ParameterDefinition<>(
					Type.SYNTAX_NODE_ID,
					"NodeId",
					"",
					"",
					1,
					1,
					true
			));
		});
	}

	@Test
	void testRegistry() {
		verify(lookUpMock, times(1)).registerOperation(testee);
	}
}