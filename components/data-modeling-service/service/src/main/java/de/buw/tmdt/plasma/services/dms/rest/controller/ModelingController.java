package de.buw.tmdt.plasma.services.dms.rest.controller;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.services.dms.core.handler.ModelingHandler;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.Modeling;
import de.buw.tmdt.plasma.services.dms.core.repository.ModelingRepository;
import de.buw.tmdt.plasma.services.dms.shared.api.DataModelingAPI;
import de.buw.tmdt.plasma.services.dms.shared.api.ModelingInfo;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@RestController("Modeling Controller")
public class ModelingController implements DataModelingAPI {

    private final Logger log = LoggerFactory.getLogger(ModelingController.class);

    private final ModelingHandler modelingHandler;
    private final ModelingRepository modelingRepository;

    @Autowired
    public ModelingController(
            @NotNull ModelingHandler modelingHandler, ModelingRepository modelingRepository) {
        this.modelingHandler = modelingHandler;
        this.modelingRepository = modelingRepository;
    }

    @Override
    @Operation(description = "Initializes a new modeling.")
    public ModelingInfo createNewModeling(@NotNull String modelId,
                                          @NotNull CombinedModel combinedModel,
                                          boolean performInitialModeling,
                                          String name,
                                          String description,
                                          String dataId) {
        Modeling modeling = modelingHandler.initializeModeling(modelId, combinedModel, name, description, dataId);
        if (performInitialModeling) {
            // TODO async?
            modelingHandler.performSemanticLabeling(modelId);
            modelingHandler.performSemanticModeling(modelId);
        }
        return new ModelingInfo(
                modeling.getId(),
                modeling.getName(),
                modeling.getDescription(),
                modeling.getDataId());
    }

    @Override
    @Operation(description = "Get all modelings.")
    public List<ModelingInfo> listModelings() {
        return modelingRepository.findAll().stream()
                .map(modeling -> new ModelingInfo(
                        modeling.getId(),
                        modeling.getName(),
                        modeling.getDescription(),
                        modeling.getDataId()))
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull CombinedModel performSemanticLabeling(@NotNull String modelId) {
        return modelingHandler.performSemanticLabeling(modelId);
    }

    @Override
    public @NotNull CombinedModel performSemanticModeling(@NotNull String modelId) {
        return modelingHandler.performSemanticModeling(modelId);
    }

    @Override
    @Operation(description = "Copy the semantic model from source model to target model")
    public CombinedModel copySemanticModelFromOtherModel(@NotNull String modelId, @NotNull String sourceModelId) {
        return dataModelingHandler.copySemanticModelFromOtherModel(modelId, sourceModelId);
    }

    @NotNull
    @Override
    @Operation(description = "Returns the current schema of the specified model.")
    public CombinedModel getCombinedModel(@NotNull String modelId) {
        CombinedModel combinedModel = modelingHandler.getCombinedModel(modelId);
        combinedModel = modelingHandler.requestRecommendations(modelId, combinedModel);
        modelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @Override
    @Operation(description = "Stores the position of all provided elements.")
    public void updatePositions(@NotNull String modelId, @NotNull List<PositionedCombinedModelElement> elements) {
        modelingHandler.updatePositions(modelId, elements);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all elements either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<SemanticModelNode> getElements(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return modelingHandler.getElements(modelId, prefix, infix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all relations either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<Relation> getRelations(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return modelingHandler.getRelations(modelId, prefix,infix);
    }

    @NotNull
    @Override
    @Operation(description = "Modify the syntactic schema of model by invoking an operation.")
    public CombinedModel modifySyntacticSchema(@NotNull String modelId, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        CombinedModel combinedModel = modelingHandler.modifySyntaxModel(modelId, syntacticOperationDTO);
        modelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional node to the cache of the model.")
    public CombinedModelElement cacheElement(@NotNull String modelId, @NotNull SemanticModelNode node) {
        return modelingHandler.cacheNode(modelId, node);
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional relation to the cache of the model.")
    public CombinedModelElement cacheRelation(@NotNull String modelId, @NotNull Relation relation) {
        return modelingHandler.cacheRelation(modelId, relation);
    }

    @NotNull
    @Operation(description = "Accepts a recommendation and adds the contents to the semantic model.")
    public CombinedModel acceptRecommendation(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = modelingHandler.applyModification(modelId, recommendation);

        // request new recommendations
        model = modelingHandler.requestRecommendations(modelId, model);
        modelingHandler.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Operation(description = "Performs a modification of the CombinedModel.")
    public CombinedModel modifyModel(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = modelingHandler.applyModification(modelId, recommendation);

        // request new recommendations
        model = modelingHandler.requestRecommendations(modelId, model);
        modelingHandler.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Override
    @Operation(description = "pop last element from model stack")
    public CombinedModel undo(@NotNull String uuid) {
        CombinedModel combinedModel = modelingHandler.undoModelModification(uuid);
        modelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "push last element from model stack")
    public CombinedModel redo(@NotNull String uuid) {
        CombinedModel combinedModel = modelingHandler.redoModelModification(uuid);
        modelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "End modelling process and finalize model.")
    public CombinedModel finalizeModeling(@NotNull String modelId) {
        return modelingHandler.finalizeModel(modelId);
    }

    @Override
    @Operation(description = "Deletes an existing modeling")
    public void deleteModel(@NotNull String modelId) {
        modelingHandler.deleteModel(modelId);
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