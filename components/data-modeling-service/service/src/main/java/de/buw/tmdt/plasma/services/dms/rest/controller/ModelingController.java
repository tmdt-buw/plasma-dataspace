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
    public List<SemanticModelNode> getElements(@NotNull String modelId, @NotNull String prefix) {
        return modelingHandler.getElements(modelId, prefix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all relations either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<Relation> getRelations(@NotNull String modelId, @NotNull String prefix) {
        return modelingHandler.getRelations(modelId, prefix);
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

    /* OLD FUNCTIONS

     @Override
    @Operation(description = "Patches the position of an arbitrary schema node.")
    public void positionSchemaNode(
            @NotNull String uuid,
            @NotNull String nodeId,
            @NotNull SchemaNode schemaNode
    ) {
        dataModelingHandler.placeSchemaNode(uuid), nodeId), schemaNode);
    }

    @NotNull
    @Override
    @Operation(description = "Adds an entity type and caches or creates the referenced entity concept if it is not present yet.")
    public CombinedModel addEntityType(
            @NotNull String dataSourceUuid,
            @NotNull Instance instance,
            @Nullable String primitiveNodeUuid
    ) {
        // modify the semantic model
        CombinedModel combinedModel = dataModelingHandler.addEntityType(dataSourceUuid), instance, primitiveNodeUuid);
        // request recommendations
        combinedModel = dataModelingHandler.requestRecommendations(dataSourceUuid, combinedModel);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Updates an entity type and caches the referenced entity concept if it is not present yet.")
    public CombinedModel updateEntityType(
            @NotNull String dataSourceUuid,
            @NotNull String entityTypeId,
            @NotNull Instance instance
    ) {
        CombinedModel combinedModel = dataModelingHandler.updateEntityType(dataSourceUuid), entityTypeId, instance);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @Override
    @Operation(description = "Patches the position of an entity type node.")
    public void positionEntityType(
            @NotNull String dataSourceUuid,
            @NotNull String entityTypeId,
            @NotNull Instance instance
    ) {
        dataModelingHandler.placeEntityType(dataSourceUuid), entityTypeId, instance);
    }

    @NotNull
    @Override
    @Operation(description = "Removes an entity type.")
    public CombinedModel removeInstance(@NotNull String dataSourceUuid, @NotNull String entityTypeId) {
        CombinedModel combinedModel = dataModelingHandler.removeEntityTypeFromModel(dataSourceUuid), entityTypeId);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Adds a relation and caches or creates the referenced relation concept if it is not present yet.")
    public CombinedModel addRelation(@NotNull String dataSourceUuid, @NotNull Relation relation) {

        CombinedModel combinedModel = dataModelingHandler.addRelation(dataSourceUuid), relation);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Updates a relation and caches the referenced relation concept if it is not present yet.")
    public CombinedModel updateRelation(
            @NotNull String dataSourceUuid,
            @NotNull String relationId,
            @NotNull Relation relationDto
    ) {
        CombinedModel combinedModel = dataModelingHandler.updateRelation(dataSourceUuid), relationId, relationDto);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Removes a relation.")
    public CombinedModel removeRelation(@NotNull String dataSourceUuid, @NotNull String entityTypeId) {
        CombinedModel combinedModel = dataModelingHandler.removeRelationFromModel(dataSourceUuid), entityTypeId);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }
     */

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
    @Operation(description = "finish modelling process")
    public CombinedModel finishModeling(@NotNull String modelId) {
        return modelingHandler.finishModeling(modelId);
    }

    @Override
    public void deleteModel(@NotNull String modelId) {
        modelingHandler.deleteModel(modelId);
    }

}