package de.buw.tmdt.plasma.services.dms.shared.api;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "/api/plasma-dms/models")
public interface DataModelingAPI {

	//############################################### Schema Definition ###############################################

	@PostMapping(value = "/init")
	CombinedModel initSchema(
			@NotNull @RequestParam("modelId") String modelId,
			@NotNull @RequestParam(value = "exampleLimit", defaultValue = "100") Integer exampleLimit
	);

	@NotNull
	@GetMapping(value = "/{modelId}", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel getCombinedModel(@NotNull @PathVariable("modelId") String modelId);

	@PatchMapping(value = "/{modelId}/positions", consumes = MediaType.APPLICATION_JSON_VALUE)
	void updatePositions(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody List<PositionedCombinedModelElement> elements);

	@NotNull
	@GetMapping(value = "/{modelId}/elements", produces = MediaType.APPLICATION_JSON_VALUE)
	List<SemanticModelNode> getElements(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestParam("prefix") String prefix);

	@NotNull
	@GetMapping(value = "/{modelId}/relations", produces = MediaType.APPLICATION_JSON_VALUE)
	List<Relation> getRelations(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestParam("prefix") String prefix);

	@NotNull
	@PostMapping(value = "/{modelId}/invoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel modifySyntacticSchema(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody SyntacticOperationDTO syntacticOperationDTO);

	/* Cache operations */

	@NotNull
	@PostMapping(value = "/{modelId}/cache/nodes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModelElement cacheElement(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody SemanticModelNode node);

	@NotNull
	@PostMapping(value = "/{modelId}/cache/relations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModelElement cacheRelation(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody Relation relation);



	@NotNull
	@PatchMapping(value = "/{modelId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel modifyModel(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody DeltaModification delta);

	/*

	@NotNull
	@PostMapping(value = "/{modelId}/entitytype", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel addEntityType(
			@NotNull @PathVariable("modelId") String modelId,
			@NotNull @RequestBody Instance entityType,
			@Nullable @RequestParam(value = "primitiveNodeId", required = false) String primitiveNodeIdString
	);

	@NotNull
	@PutMapping(value = "/{modelId}/entitytype/{entityTypeId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel updateEntityType(
			@NotNull @PathVariable("modelId") String modelId,
			@NotNull @PathVariable("entityTypeId") String entityTypeId,
			@NotNull @RequestBody Instance instance
	);

	@PatchMapping(value = "/{modelId}/entitytype/{entityTypeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	void positionEntityType(
			@NotNull @PathVariable("modelId") String modelId,
			@NotNull @PathVariable("entityTypeId") String entityTypeId,
			@NotNull @RequestBody Instance instance
	);

	@NotNull
	@DeleteMapping(value = "/{modelId}/entitytype/{entityTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel removeInstance(@NotNull @PathVariable("modelId") String modelId, @NotNull @PathVariable("entityTypeId") String entityTypeId);


	@NotNull
	@PostMapping(value = "/{modelId}/relation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel addRelation(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody Relation relationDto);

	@NotNull
	@PutMapping(value = "/{modelId}/relation/{relationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel updateRelation(
			@NotNull @PathVariable("modelId") String modelId,
			@NotNull @PathVariable("relationId") String entityTypeId,
			@NotNull @RequestBody Relation relationDto
	);

	@NotNull
	@DeleteMapping(value = "/{modelId}/relation/{relationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel removeRelation(@NotNull @PathVariable("modelId") String modelId, @NotNull @PathVariable("relationId") String entityTypeId);

	 */
	/* Recommendation */

	@NotNull
	@PostMapping(value = "/{modelId}/recommendations/accept", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel acceptRecommendation(@NotNull @PathVariable("modelId") String modelId, @NotNull @RequestBody DeltaModification recommendation);

	/* Undo / Redo / finish */

	@NotNull
	@PostMapping(value = "/{modelId}/undo", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel undo(@NotNull @PathVariable("modelId") String modelId);

	@NotNull
	@PostMapping(value = "/{modelId}/redo", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel redo(@NotNull @PathVariable("modelId") String modelId);

	@NotNull
	@PostMapping(value = "/{modelId}/finalize")
	CombinedModel finalizeModeling(@NotNull @PathVariable("modelId") String modelId);

	@DeleteMapping(value = "/{modelId}")
	void deleteModel(@NotNull @PathVariable("modelId") String modelId);
}
