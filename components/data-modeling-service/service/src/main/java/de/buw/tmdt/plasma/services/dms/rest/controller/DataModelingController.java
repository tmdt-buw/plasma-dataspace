package de.buw.tmdt.plasma.services.dms.rest.controller;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.services.dms.core.handler.DataModelingHandler;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.shared.api.DataModelingAPI;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


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
    @Operation(description = "Initializes the current schema of the specified data source.")
    public void initDataSourceSchema(@NotNull String dataSourceUuid, @NotNull Integer exampleLimit) {
        dataModelingHandler.initDataSourceSchema(UUID.fromString(dataSourceUuid), exampleLimit);
    }

    @NotNull
    @Override
    @Operation(description = "Returns the current schema of the specified data source.")
    public CombinedModel getCombinedModel(@NotNull String dataSourceUuid) {
        CombinedModel combinedModel = dataModelingHandler.getCombinedModel(UUID.fromString(dataSourceUuid));
        combinedModel = dataModelingHandler.requestRecommendations(dataSourceUuid, combinedModel);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @Override
    @Operation(description = "Stores the position of all provided elements.")
    public void updatePositions(@NotNull String dataSourceUuid, @NotNull List<PositionedCombinedModelElement> elements) {
        dataModelingHandler.updatePositions(UUID.fromString(dataSourceUuid), elements);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all entity concepts either persistent in the knowledge base or cached for the referenced data source sorted alphabetically.")
    public List<EntityConcept> getEntityConcepts(@NotNull String dataSourceUuid, @NotNull String prefix) {
        return dataModelingHandler.getEntityConcepts(UUID.fromString(dataSourceUuid), prefix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all entity concepts either persistent in the knowledge base or cached for the referenced data source sorted alphabetically.")
    public List<RelationConcept> getRelationConcepts(@NotNull String dataSourceUuid, @NotNull String prefix) {
        return dataModelingHandler.getRelationConcepts(UUID.fromString(dataSourceUuid), prefix);
    }

    @NotNull
    @Override
    @Operation(description = "Modify the syntactic schema of a datasource by invoking an operation.")
    public CombinedModel modifySchema(@NotNull String dataSourceUuid, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        CombinedModel combinedModel = dataModelingHandler.modifySyntaxModel(UUID.fromString(dataSourceUuid), syntacticOperationDTO);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @Override
    @Operation(description = "Patches the position of an arbitrary schema node.")
    public void positionSchemaNode(
            @NotNull String dataSourceUuid,
            @NotNull String schemaNodeUuid,
            @NotNull SchemaNode schemaNode
    ) {
        dataModelingHandler.placeSchemaNode(UUID.fromString(dataSourceUuid), UUID.fromString(schemaNodeUuid), schemaNode);
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional entity concept to the cache of the data source.")
    public EntityConcept cacheEntityConcept(@NotNull String dataSourceUuid, @NotNull EntityConcept entityConcept) {
        return dataModelingHandler.cacheEntityConcept(UUID.fromString(dataSourceUuid), entityConcept);
    }

    @NotNull
    @Override
    @Operation(description = "Removes a provisional entity concept from the cache of the data source.")
    public List<EntityConcept> uncacheEntityConcept(@NotNull String dataSourceUuid, @NotNull String entityConceptId) {
        dataModelingHandler.uncacheEntityConcept(UUID.fromString(dataSourceUuid), entityConceptId);
        return dataModelingHandler.getEntityConcepts(UUID.fromString(dataSourceUuid), "");
    }

    @NotNull
    @Override
    @Operation(description = "Adds a provisional relation concept to the cache of the data source.")
    public RelationConcept cacheRelationConcept(@NotNull String dataSourceUuid, @NotNull RelationConcept relationConcept) {
        return dataModelingHandler.cacheRelationConcept(UUID.fromString(dataSourceUuid), relationConcept);
    }

    @NotNull
    @Override
    @Operation(description = "Removes a temporary entity concept from the cache of the data source.")
    public List<RelationConcept> uncacheRelationConcept(@NotNull String dataSourceUuid, @NotNull String relationConceptId) {
        dataModelingHandler.uncacheRelationConcept(UUID.fromString(dataSourceUuid), relationConceptId);
        return dataModelingHandler.getRelationConcepts(UUID.fromString(dataSourceUuid), "");
    }

    @NotNull
    @Override
    @Operation(description = "Adds an entity type and caches or creates the referenced entity concept if it is not present yet.")
    public CombinedModel addEntityType(
            @NotNull String dataSourceUuid,
            @NotNull EntityType entityType,
            @Nullable String primitiveNodeUuid
    ) {
        // modify the semantic model
        CombinedModel combinedModel = dataModelingHandler.addEntityType(UUID.fromString(dataSourceUuid), entityType, primitiveNodeUuid);
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
            @NotNull EntityType entityType
    ) {
        CombinedModel combinedModel = dataModelingHandler.updateEntityType(UUID.fromString(dataSourceUuid), entityTypeId, entityType);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Operation(description = "Accepts a recommendation and adds the contents to the semantic model.")
    public CombinedModel acceptRecommendation(@NotNull String dataSourceUuid, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = dataModelingHandler.applyModification(UUID.fromString(dataSourceUuid), recommendation);

        // request recommendations
        model = dataModelingHandler.requestRecommendations(dataSourceUuid, model);
        dataModelingHandler.prepareForFrontend(model);
        return model;
    }

    @Override
    @Operation(description = "Patches the position of an entity type node.")
    public void positionEntityType(
            @NotNull String dataSourceUuid,
            @NotNull String entityTypeId,
            @NotNull EntityType entityType
    ) {
        dataModelingHandler.placeEntityType(UUID.fromString(dataSourceUuid), entityTypeId, entityType);
    }

    @NotNull
    @Override
    @Operation(description = "Removes an entity type.")
    public CombinedModel removeEntityType(@NotNull String dataSourceUuid, @NotNull String entityTypeId) {
        CombinedModel combinedModel = dataModelingHandler.removeEntityTypeFromModel(UUID.fromString(dataSourceUuid), entityTypeId);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Adds a relation and caches or creates the referenced relation concept if it is not present yet.")
    public CombinedModel addRelation(@NotNull String dataSourceUuid, @NotNull Relation relation) {

        CombinedModel combinedModel = dataModelingHandler.addRelation(UUID.fromString(dataSourceUuid), relation);
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
        CombinedModel combinedModel = dataModelingHandler.updateRelation(UUID.fromString(dataSourceUuid), relationId, relationDto);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Removes a relation.")
    public CombinedModel removeRelation(@NotNull String dataSourceUuid, @NotNull String entityTypeId) {
        CombinedModel combinedModel = dataModelingHandler.removeRelationFromModel(UUID.fromString(dataSourceUuid), entityTypeId);
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "pop last element from datasource's schema stack")
    public CombinedModel undoModification(@NotNull String dataSourceUuid) {
        CombinedModel combinedModel = dataModelingHandler.undoModelModification(UUID.fromString(dataSourceUuid));
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "push last element from datasource's schema stack")
    public CombinedModel redoModification(@NotNull String dataSourceUuid) {
        CombinedModel combinedModel = dataModelingHandler.redoModelModification(UUID.fromString(dataSourceUuid));
        dataModelingHandler.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "finish schema modelling process")
    public CombinedModel finishModeling(@NotNull String dataSourceUuid) {
        UUID uuid = UUID.fromString(dataSourceUuid);
        return dataModelingHandler.finishModeling(uuid);
    }

    @Override
    public void deleteModel(@NotNull String dataSourceUuid) {
        UUID uuid = UUID.fromString(dataSourceUuid);
        dataModelingHandler.deleteModel(uuid);
    }
}