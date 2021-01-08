package de.buw.tmdt.plasma.services.dms.shared.api;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "/api/plasma-dms/modeling")
public interface DataModelingAPI {

	//############################################### Schema Definition ###############################################

	@PostMapping(value = "/init")
	void initDataSourceSchema(
			@NotNull @RequestParam("uuid") String uuid,
			@NotNull @RequestParam(value = "exampleLimit", defaultValue = "100") Integer exampleLimit
	);

	@NotNull
	@GetMapping(value = "/{uuid}/schema", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel getCombinedModel(@NotNull @PathVariable("uuid") String uuid);

	@PatchMapping(value = "/{uuid}/schema", consumes = MediaType.APPLICATION_JSON_VALUE)
	void updatePositions(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody List<PositionedCombinedModelElement> elements);

	@NotNull
	@GetMapping(value = "/{uuid}/entityconcepts", produces = MediaType.APPLICATION_JSON_VALUE)
	List<EntityConcept> getEntityConcepts(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestParam("prefix") String prefix);

	@NotNull
	@GetMapping(value = "/{uuid}/relationconcepts", produces = MediaType.APPLICATION_JSON_VALUE)
	List<RelationConcept> getRelationConcepts(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestParam("prefix") String prefix);

	@NotNull
	@PostMapping(value = "/{uuid}/invoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel modifySchema(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody SyntacticOperationDTO syntacticOperationDTO);

	@PatchMapping(value = "/{dataSourceUuid}/syntaxmodel/{schemaNodeUuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
	void positionSchemaNode(
			@NotNull @PathVariable("dataSourceUuid") String dataSourceUuid,
			@NotNull @PathVariable("schemaNodeUuid") String schemaNodeUuid,
			@NotNull @RequestBody SchemaNode schemaNodeDTO
	);

	/* Cache operations */

	@NotNull
	@PostMapping(value = "/{uuid}/entityconcept", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	EntityConcept cacheEntityConcept(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody EntityConcept entityConceptDTO);

	@NotNull
	@DeleteMapping(value = "/{dataSourceUuid}/entityconcept/{entityConceptId}", produces = MediaType.APPLICATION_JSON_VALUE)
	List<EntityConcept> uncacheEntityConcept(
			@NotNull @PathVariable("dataSourceUuid") String dataSourceUuid,
			@NotNull @PathVariable("entityConceptId") String entityConceptId
	);

	@NotNull
	@PostMapping(value = "/{uuid}/relationconcept", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	RelationConcept cacheRelationConcept(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody RelationConcept relationConceptDTO);

	@NotNull
	@DeleteMapping(value = "/{dataSourceUuid}/relationconcept/{relationConceptId}", produces = MediaType.APPLICATION_JSON_VALUE)
	List<RelationConcept> uncacheRelationConcept(
			@NotNull @PathVariable("dataSourceUuid") String dataSourceUuid,
			@NotNull @PathVariable("relationConceptId") String relationConceptId
	);

	/* Modeling operations (EntityTypes)*/

	@NotNull
	@PostMapping(value = "/{uuid}/entitytype", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel addEntityType(
			@NotNull @PathVariable("uuid") String uuid,
			@NotNull @RequestBody EntityType entityType,
			@Nullable @RequestParam(value = "primitiveNodeId", required = false) String primitiveNodeIdString
	);

	@NotNull
	@PutMapping(value = "/{uuid}/entitytype/{entityTypeId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel updateEntityType(
			@NotNull @PathVariable("uuid") String uuid,
			@NotNull @PathVariable("entityTypeId") String entityTypeId,
			@NotNull @RequestBody EntityType entityTypeDTO
	);

	@PatchMapping(value = "/{uuid}/entitytype/{entityTypeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	void positionEntityType(
			@NotNull @PathVariable("uuid") String uuid,
			@NotNull @PathVariable("entityTypeId") String entityTypeId,
			@NotNull @RequestBody EntityType entityTypeDTO
	);

	@NotNull
	@DeleteMapping(value = "/{uuid}/entitytype/{entityTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel removeEntityType(@NotNull @PathVariable("uuid") String uuid, @NotNull @PathVariable("entityTypeId") String entityTypeId);

	/* Modeling operations (Relations)*/

	@NotNull
	@PostMapping(value = "/{uuid}/relation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel addRelation(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody Relation relationDto);

	@NotNull
	@PutMapping(value = "/{uuid}/relation/{relationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel updateRelation(
			@NotNull @PathVariable("uuid") String uuid,
			@NotNull @PathVariable("relationId") String entityTypeId,
			@NotNull @RequestBody Relation relationDto
	);

	@NotNull
	@DeleteMapping(value = "/{uuid}/relation/{relationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel removeRelation(@NotNull @PathVariable("uuid") String uuid, @NotNull @PathVariable("relationId") String entityTypeId);

	/* Recommendation */

	@NotNull
	@PostMapping(value = "/{uuid}/recommendation/accept", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel acceptRecommendation(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody DeltaModification recommendation);

	/* Undo / Redo / finish */

	@NotNull
	@PostMapping(value = "/{uuid}/schema/undo", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel undoModification(@NotNull @PathVariable("uuid") String uuid);

	@NotNull
	@PostMapping(value = "/{uuid}/schema/redo", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel redoModification(@NotNull @PathVariable("uuid") String uuid);

	@NotNull
	@PostMapping(value = "/{uuid}/schema/finish")
	CombinedModel finishModeling(@NotNull @PathVariable("uuid") String uuid);

	@DeleteMapping(value = "/{uuid}")
	void deleteModel(@NotNull @PathVariable("uuid") String uuid);
}
