package de.buw.tmdt.plasma.services.dms.shared.api;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "/api/plasma-dms/models")
public interface DataModelingAPI {

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
    List<SemanticModelNode> getElements(@NotNull @PathVariable("modelId") String modelId,
                                        @Nullable @RequestParam(value = "prefix", required = false) String prefix,
                                        @Nullable @RequestParam(value = "infix", required = false) String infix);

    @NotNull
    @GetMapping(value = "/{modelId}/relations", produces = MediaType.APPLICATION_JSON_VALUE)
    List<Relation> getRelations(@NotNull @PathVariable("modelId") String modelId,
                                @Nullable @RequestParam(value = "prefix", required = false) String prefix,
                                @Nullable @RequestParam(value = "infix", required = false) String infix);

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

    @GetMapping(value= "/{modelId}/selectedOntologies", produces = MediaType.APPLICATION_JSON_VALUE)
    List<String> getSelectedOntologies(@NotNull @PathVariable("modelId") String modelId);

    @PutMapping(value= "/{modelId}/selectedOntologies")
    void updateSelectedOntologies(@NotNull @PathVariable("modelId") String modelId,
                                  @NotNull @RequestParam(value = "labels", defaultValue = "") List<String> selectedOntologyLabels);
}