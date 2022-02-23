package de.buw.tmdt.plasma.services.dms.rest.controller;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.URIDefinedElement;
import de.buw.tmdt.plasma.services.dms.core.handler.DataModelingHandler;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.shared.api.DataModelingAPI;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void initSchema(@NotNull String modelId, @NotNull Integer exampleLimit) {
        dataModelingHandler.initModelSchema(modelId, exampleLimit);
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
    public List<URIDefinedElement> getElements(@NotNull String modelId, @NotNull String prefix) {
        return dataModelingHandler.getElements(modelId, prefix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all relations either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<Relation> getRelations(@NotNull String modelId, @NotNull String prefix) {
        return dataModelingHandler.getRelations(modelId, prefix);
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
    @Operation(description = "Adds a new provisional element or relation to the cache of the model.")
    public CombinedModelElement cacheElementOrRelation(@NotNull String modelId, @NotNull CombinedModelElement element) {
        if (element instanceof URIDefinedElement) {
            return dataModelingHandler.cacheElement(modelId, (URIDefinedElement) element);
        }
        if (element instanceof Relation) {
            return dataModelingHandler.cacheRelation(modelId, (Relation) element);
        }
        throw new IllegalArgumentException("Unknown object type to cache " + element);
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
    @Operation(description = "finish modelling process")
    public CombinedModel finishModeling(@NotNull String modelId) {
        return dataModelingHandler.finishModeling(modelId);
    }

    @Override
    public void deleteModel(@NotNull String modelId) {
        dataModelingHandler.deleteModel(modelId);
    }
}