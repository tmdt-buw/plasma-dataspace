package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.datamodel.*;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticNode;
import de.buw.tmdt.plasma.services.sms.ars.ARSService;
import de.buw.tmdt.plasma.services.sms.modeling.ModelingService;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import de.buw.tmdt.plasma.services.sms.modeling.database.ModelingRepository;
import de.buw.tmdt.plasma.services.sms.shared.api.ModelingAPI;
import de.buw.tmdt.plasma.services.sms.shared.api.ModelingInfo;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController("Modeling Controller")
public class ModelingController implements ModelingAPI {

    private final Logger log = LoggerFactory.getLogger(ModelingController.class);

    private final ModelingService modelingService;
    private final ModelingRepository modelingRepository;
    private final ARSService arsService;

    @Value("${plasma.sms.feature.finalizemodels.enabled:true}")
    private Boolean finalizeModelsEnabled;

    @Autowired
    public ModelingController(
            @NotNull ModelingService modelingService, ModelingRepository modelingRepository, ARSService arsService) {
        this.modelingService = modelingService;
        this.modelingRepository = modelingRepository;
        this.arsService = arsService;
    }

    @Override
    @Operation(description = "Initializes a new modeling.")
    public ModelingInfo createNewModeling(@Nullable String modelId,
                                          @NotNull CombinedModel combinedModel,
                                          boolean performInitialModeling,
                                          String name,
                                          String description,
                                          String dataId) {
        if (modelId == null) {
            modelId = UUID.randomUUID().toString();
        }
        Modeling modeling = modelingService.initializeModeling(modelId, combinedModel, name, description, dataId);
        if (performInitialModeling) {
            try {
                // modelingService.performSemanticLabeling(modelId);
                // modelingService.performSemanticModeling(modelId);
            } catch (ResponseStatusException ignore) {
                // do nothing here as we still want to return the model
            }
        }
        return new ModelingInfo(
                modeling.getId(),
                modeling.getName(),
                modeling.getDescription(),
                modeling.getCreated(),
                modeling.getDataId(),
                false);
    }

    @Override
    @Operation(description = "Get all modelings.")
    public List<ModelingInfo> listModelings() {
        return modelingRepository.findAll().stream()
                .map(modeling -> new ModelingInfo(
                        modeling.getId(),
                        modeling.getName(),
                        modeling.getDescription(),
                        modeling.getCreated(),
                        modeling.getDataId(),
                        modeling.isFinalized()))
                .sorted(Comparator.comparing(ModelingInfo::getCreated).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Operation(description = "Update secondary information of a modeling.")
    public ModelingInfo updateModeling(@NotNull String modelId, @Nullable String name, @Nullable String description, @Nullable String dataId) {
        Modeling updated = modelingService.updateModeling(modelId, name, description, dataId);
        return new ModelingInfo(
                updated.getId(),
                updated.getName(),
                updated.getDescription(),
                updated.getCreated(),
                updated.getDataId(),
                updated.getCurrentModel().isFinalized());
    }

    @Override
    public @NotNull CombinedModel performSemanticLabeling(@NotNull String modelId) {
        return modelingService.performSemanticLabeling(modelId);
    }

    @Override
    public @NotNull CombinedModel performSemanticModeling(@NotNull String modelId) {
        return modelingService.performSemanticModeling(modelId);
    }

    @Override
    @Operation(description = "Copy the semantic model from source model to target model")
    public @NotNull CombinedModel copySemanticModelFromOtherModel(@NotNull String modelId, @NotNull String sourceModelId) {
        return modelingService.copySemanticModelFromOtherModel(modelId, sourceModelId);
    }

    @NotNull
    @Override
    @Operation(description = "Returns the current combined model of the specified modeling.")
    public CombinedModel getCombinedModel(@NotNull String modelId) {
        CombinedModel combinedModel = modelingService.getCombinedModel(modelId);
        if (!combinedModel.isFinalized()) {
            combinedModel = modelingService.requestRecommendations(modelId, combinedModel);
        }
        modelingService.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "Requests all array context represented by element ids (nodes and edges) for the given model")
    public List<ArrayContext> getArrayContexts(@NotNull String modelId) {
        return modelingService.getArrayContexts(modelId);
    }

    @NotNull
    @Override
    @Operation(description = "Requests all model mappings for the given model")
    public List<ModelLinking> getModelMappings(@NotNull String modelId) {
        return modelingService.getModelMappings(modelId);
    }

    @Override
    @Operation(description = "Stores the position of all provided elements.")
    public void updatePositions(@NotNull String modelId, @NotNull List<PositionedCombinedModelElement> elements) {
        modelingService.updatePositions(modelId, elements);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all elements either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<SemanticNode> getElements(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return modelingService.getElements(modelId, prefix, infix);
    }

    @NotNull
    @Override
    @Operation(description = "Returns all relations either persistent in the knowledge base or cached for the referenced model sorted alphabetically.")
    public List<Relation> getRelations(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        return modelingService.getRelations(modelId, prefix, infix);
    }

    @NotNull
    @Override
    @Operation(description = "Adds a new provisional node to the cache of the modeling.")
    public CombinedModelElement cacheElement(@NotNull String modelingId, @NotNull SemanticNode node) {
        return modelingService.cacheNode(modelingId, node);
    }

    @NotNull
    @Override
    @Operation(description = "Updates provisional node in the cache of the modeling.")
    public CombinedModelElement updateProvisionalNode(@NotNull String modelingId, @NotNull SemanticNode node, @Nullable String oldURI) {
        return modelingService.updateCachedNode(modelingId, node, oldURI);
    }

    @Override
    @Operation(description = "Removes a cached element identified by the given URI")
    public void deleteCachedElement(@NotNull String modelId, @NotNull String uri) {
        modelingService.uncacheNode(modelId, uri);
    }


    @NotNull
    @Override
    @Operation(description = "Adds a new provisional relation to the cache of the modeling.")
    public CombinedModelElement cacheRelation(@NotNull String modelingId, @NotNull Relation relation) {
        return modelingService.cacheRelation(modelingId, relation);
    }


    @NotNull
    @Override
    @Operation(description = "Updates provisional relation in the cache of the modeling.")
    public CombinedModelElement updateProvisionalRelation(@NotNull String modelingId, @NotNull Relation relation, @Nullable String oldURI) {
        return modelingService.updateCachedRelation(modelingId, relation, oldURI);
    }


    @Override
    @Operation(description = "Removes a cached relation identified by the given URI")
    public void deleteCachedRelation(@NotNull String modelId, @NotNull String uri) {
        modelingService.uncacheRelation(modelId, uri);
    }

    @NotNull
    @Operation(description = "Accepts a recommendation and adds the contents to the semantic model.")
    public CombinedModel acceptRecommendation(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = modelingService.applyModification(modelId, recommendation);

        modelingService.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Operation(description = "Performs a modification of the CombinedModel.")
    public CombinedModel modifyModel(@NotNull String modelId, @NotNull DeltaModification recommendation) {
        @NotNull CombinedModel model = modelingService.applyModification(modelId, recommendation);

        modelingService.prepareForFrontend(model);
        return model;
    }

    @NotNull
    @Override
    @Operation(description = "pop last element from model stack")
    public CombinedModel undo(@NotNull String uuid) {
        CombinedModel combinedModel = modelingService.undoModelModification(uuid);
        modelingService.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "push last element from model stack")
    public CombinedModel redo(@NotNull String uuid) {
        CombinedModel combinedModel = modelingService.redoModelModification(uuid);
        modelingService.prepareForFrontend(combinedModel);
        return combinedModel;
    }

    @NotNull
    @Override
    @Operation(description = "End modelling process and finalize model.")
    public CombinedModel finalizeModeling(@NotNull String modelId) {
        return modelingService.finalizeModeling(modelId);
    }

    @NotNull
    @Override
    @Operation(description = "Clone an existing modeling.")
    public ModelingInfo cloneModeling(@NotNull String modelId) {
        Modeling modeling = modelingService.cloneModeling(modelId);
        return new ModelingInfo(
                modeling.getId(),
                modeling.getName(),
                modeling.getDescription(),
                modeling.getCreated(),
                modeling.getDataId(),
                false);
    }

    @NotNull
    @Override
    @Operation(description = "Check if a model may be finalized. This can be disabled in the DMS config.")
    public Boolean finalizeModelingAvailable() {
        return finalizeModelsEnabled;
    }

    @Override
    @Operation(description = "Deletes an existing modeling")
    public void deleteModel(@NotNull String modelId) {
        modelingService.deleteModel(modelId);
    }

    @Override
    public List<String> getSelectedOntologies(@NotNull String modelId) {
        return modelingService.getSelectedOntologies(modelId);
    }

    @Override
    public void updateSelectedOntologies(@NotNull String modelId, @NotNull List<String> selectedOntologyLabels) {
        modelingService.updateSelectedOntologies(modelId, selectedOntologyLabels);
    }

    @Override
    @Operation(description = "Updates the preferred services for a model")
    public void updatePreferredServices(@NotNull String modelId, PreferredServices preferredServices) {
        modelingService.updatePreferredServices(modelId, preferredServices);
    }

    @Override
    public PreferredServices getPreferredServices(@NotNull String modelId) {
        return modelingService.getPreferredServices(modelId);
    }
}