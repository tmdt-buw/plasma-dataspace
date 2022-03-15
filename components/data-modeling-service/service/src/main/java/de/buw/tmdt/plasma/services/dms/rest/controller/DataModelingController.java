package de.buw.tmdt.plasma.services.dms.rest.controller;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.services.dms.core.handler.DataModelingHandler;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.shared.api.DataModelingAPI;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController("Modeling Controller")
public class DataModelingController implements DataModelingAPI {

    private final Logger log = LoggerFactory.getLogger(DataModelingController.class);

    private final DataModelingHandler dataModelingHandler;
    private final OperationLookUp operationLookUp;

    @Autowired
    public DataModelingController(
            @NotNull DataModelingHandler dataModelingHandler,
            OperationLookUp operationLookUp) {
        this.dataModelingHandler = dataModelingHandler;
        this.operationLookUp = operationLookUp;
    }

    @Override
    @Operation(description = "Initializes the current schema of the specified model.")
    public CombinedModel initSchema(@NotNull String modelId, @NotNull Integer exampleLimit) {
        dataModelingHandler.initModel(modelId, exampleLimit);
        return getCombinedModel(modelId);
    }

    @NotNull
    @Override
    @Operation(description = "Returns the current schema of the specified model.")
    public CombinedModel getCombinedModel(@NotNull String modelId) {
        CombinedModel combinedModel = dataModelingHandler.getCombinedModel(modelId);
        combinedModel = dataModelingHandler.requestRecommendations(modelId, combinedModel);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @Override
    @Operation(description = "Stores the position of all provided elements.")
    public void updatePositions(@NotNull String modelId, @NotNull List<PositionedCombinedModelElement> elements) {
        dataModelingHandler.updatePositions(modelId, elements);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all elements either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<SemanticModelNode> getElements(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return dataModelingHandler.getElements(modelId, prefix, infix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all relations either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<Relation> getRelations(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return dataModelingHandler.getRelations(modelId, prefix,infix);
    }

    @NotNull
    @Override
    @Operation(description = "Modify the syntactic schema of model by invoking an operation.")
    public CombinedModel modifySyntacticSchema(@NotNull String modelId, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        CombinedModel combinedModel = dataModelingHandler.modifySyntaxModel(modelId, syntacticOperationDTO);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional node to the cache of the model.")
    public CombinedModelElement cacheElement(@NotNull String modelId, @NotNull SemanticModelNode node) {
            return dataModelingHandler.cacheNode(modelId, node);
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional relation to the cache of the model.")
    public CombinedModelElement cacheRelation(@NotNull String modelId, @NotNull Relation relation) {
        return dataModelingHandler.cacheRelation(modelId, relation);
    }

    @NotNull
    @Operation(description = "Accepts a recommendation and adds the contents to the semantic model.")
    public CombinedModel acceptRecommendation(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = dataModelingHandler.applyModification(modelId, recommendation);

        // request new recommendations
        model = dataModelingHandler.requestRecommendations(modelId, model);
        dataModelingHandler.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Operation(description = "Performs a modification of the CombinedModel.")
    public CombinedModel modifyModel(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = dataModelingHandler.applyModification(modelId, recommendation);

        // request new recommendations
        model = dataModelingHandler.requestRecommendations(modelId, model);
        dataModelingHandler.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Override
    @Operation(description = "pop last element from model stack")
    public CombinedModel undo(@NotNull String uuid) {
        CombinedModel combinedModel = dataModelingHandler.undoModelModification(uuid);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "push last element from model stack")
    public CombinedModel redo(@NotNull String uuid) {
        CombinedModel combinedModel = dataModelingHandler.redoModelModification(uuid);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "End modelling process and finalize model.")
    public CombinedModel finalizeModeling(@NotNull String modelId) {
        return dataModelingHandler.finalizeModel(modelId);
    }

    @Override
    @Operation(description = "Deletes an existing modeling")
    public void deleteModel(@NotNull String modelId) {
        dataModelingHandler.deleteModel(modelId);
    }

    @Override
    public List<String> getSelectedOntologies(@NotNull String modelId){
        return dataModelingHandler.getSelectedOntologies(modelId);
    }

    @Override
    public void updateSelectedOntologies(@NotNull String modelId, @NotNull List<String> selectedOntologyLabels){
        dataModelingHandler.updateSelectedOntologies(modelId, selectedOntologyLabels);
    }
}