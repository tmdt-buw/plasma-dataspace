package de.buw.tmdt.plasma.services.dms.core.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.config.ConfigServiceClient;
import de.buw.tmdt.plasma.services.dms.core.converter.SchemaOperationDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.operations.Operation;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.operations.exceptions.ParameterParsingException;
import de.buw.tmdt.plasma.services.dms.core.repository.DataSourceRepository;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.OntologyApiClient;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.SemanticModelApiClient;
import de.buw.tmdt.plasma.services.sas.shared.feignclient.SchemaAnalysisApiClient;
import de.buw.tmdt.plasma.services.srs.feignclient.SemanticRecommendationApiClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DataModelingHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataModelingHandler.class);

    private final SemanticModelApiClient semanticModelApiClient;
    private final OntologyApiClient ontologyApiClient;
    private final DataSourceRepository dataSourceRepository;
    private final SchemaAnalysisApiClient schemaAnalysisApiClient;
    private final SemanticRecommendationApiClient semanticRecommendationApiClient;
    private final SchemaOperationDTOConverter schemaOperationDTOConverter;
    private final OperationLookUp operationLookUp;
    private HashMap<SchemaNode, Set<Operation.Handle>> parameterDefinitionLookup;
    private final ConfigServiceClient configServiceClient;

    @Autowired
    public DataModelingHandler(
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull SemanticModelApiClient semanticModelApiClient,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull OntologyApiClient ontologyApiClient,
            @NotNull DataSourceRepository dataSourceRepository,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull SchemaAnalysisApiClient schemaAnalysisApiClient,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull SemanticRecommendationApiClient semanticRecommendationApiClient,
            @NotNull SchemaOperationDTOConverter schemaOperationDTOConverter,
            @NotNull OperationLookUp operationLookUp, ConfigServiceClient configServiceClient) {
        this.semanticModelApiClient = semanticModelApiClient;
        this.ontologyApiClient = ontologyApiClient;
        this.dataSourceRepository = dataSourceRepository;
        this.schemaAnalysisApiClient = schemaAnalysisApiClient;
        this.semanticRecommendationApiClient = semanticRecommendationApiClient;
        this.schemaOperationDTOConverter = schemaOperationDTOConverter;
        this.operationLookUp = operationLookUp;
        this.configServiceClient = configServiceClient;
    }

    public void initModelSchema(@NotNull String modelId, int exampleLimit) {
        DataSourceModel dataSourceModel = dataSourceRepository.findById(UUID.fromString(modelId)).orElse(null);
        if (dataSourceModel != null) {
            logger.info("Received init request for already existing data source, skipping...");
            return;
        }
        Optional<DataSourceModel> dataSourceResult = dataSourceRepository.findById(UUID.fromString(modelId));

        if (dataSourceResult.isPresent()) {
            logger.info("Data source already exists - the current schema is replaced with the last existing one in the SAS");
            dataSourceModel = dataSourceResult.get();
            //dataSourceModel.setDataSourceSchemaStack(Collections.emptyList());
        } else {
            logger.info("Data source does not exist");
            DataSourceModel dsm = new DataSourceModel(UUID.fromString(modelId));
            dataSourceModel = dataSourceRepository.save(dsm);
            logger.info("Created a new data source");
        }
        dataSourceModel.pushCombinedModel(createInitialSchema(modelId, exampleLimit, dataSourceModel));

        //store the schema
        dataSourceRepository.save(dataSourceModel);
    }

    private CombinedModel createInitialSchema(String modelId, int exampleLimit, DataSourceModel dataSourceModel) {
        @NotNull SyntaxModel syntaxModel;
        try {
            syntaxModel = schemaAnalysisApiClient.getResult(modelId.toString(), exampleLimit);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach SchemaAnalysisService", e);
        }
        SemanticModel semanticModel;
        semanticModel = new SemanticModel(
                null,
                new ArrayList<>(),
                new ArrayList<>()
        );

        CombinedModel syntaxCombinedModel = new CombinedModel(
                modelId,
                syntaxModel,
                semanticModel,
                null,
                false
        );

        dataSourceModel.pushCombinedModel(syntaxCombinedModel);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);

        // request semantic labeling
        CombinedModel combinedModel = dataSourceModel.getCurrentModel();
        combinedModel.getSyntaxModel().getNodes().forEach(node -> {
            // no operations when sending to other services
            node.getOperations().clear();
        });

        try {
            CombinedModel labeledCombinedModel;
            labeledCombinedModel = semanticRecommendationApiClient.performSemanticLabeling(
                    modelId.toString(), null, null, combinedModel);
            try {
                CombinedModel modeledCombinedModel;
                // request semantic modeling
                modeledCombinedModel = semanticRecommendationApiClient.performSemanticModeling(
                        modelId.toString(), null, null, labeledCombinedModel);
                return modeledCombinedModel;
            } catch (Exception e) {
                logger.warn("Semantic modeling did not succeed: ", e);
                logger.warn("Continuing with labeled model...");
            }
            return labeledCombinedModel;
        } catch (Exception e) {
            logger.warn("Semantic labeling did not succeed: ", e);
            logger.warn("Continuing with raw model...");
        }
        return dataSourceModel.getCurrentModel();
    }

    public @NotNull CombinedModel getCombinedModel(@NotNull String modelId) {

        return getCombinedModel(modelId, false);
    }

    /**
     * Retrieves all available {@link SemanticModelNode}s, from the KGS as well as the current model.
     *
     * @param modelId The data source uuid
     * @param prefix  A search prefix
     * @return The resulting list of matching {@link SemanticModelNode}s
     */
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
    @NotNull
    public List<SemanticModelNode> getElements(@NotNull String modelId, @NotNull String prefix) {
        //Retrieve the concepts from the KGS
        List<SemanticModelNode> availableElements;
        try {
            availableElements =
                    ontologyApiClient.getElements(prefix, null, null);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach KnowledgeGraphService");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findDataSourceOrThrow(modelId), false);
        combinedModel.getProvisionalElements().stream()
                .filter(element -> element.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(element -> availableElements.stream().noneMatch(ae -> ae.sameAs(element)))
                .forEach(availableElements::add);

        return availableElements.stream()
                .sorted(Comparator.comparing(SemanticModelNode::getLabel))
                .collect(Collectors.toList());
    }

    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
    @NotNull
    public List<Relation> getRelations(@NotNull String modelId, @NotNull String prefix) {
        // Retrieve the concepts from the KGS
        List<Relation> availableRelations;
        try {
            availableRelations =
                    ontologyApiClient.getRelations(prefix, null, null);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach KnowledgeGraphService");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findDataSourceOrThrow(modelId), false);
        combinedModel.getProvisionalRelations().stream()
                .filter(relationConcept -> relationConcept.getLabel().startsWith(prefix))
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(relationConcept -> availableRelations.stream().noneMatch(ar -> ar.sameAs(relationConcept)))
                .forEach(availableRelations::add);

        return availableRelations.stream()
                .sorted(Comparator.comparing(Relation::getLabel))
                .collect(Collectors.toList());
    }

    /* SYNTAX NODE OPERATIONS */

    /**
     * Updates the position of a {@link SchemaNode} in the {@link CombinedModel}.
     *
     * @param modelId The data source uuid identifying the {@link CombinedModel}
     * @param schemaNode     The {@link SchemaNode} to place
     * @param schemaNodeUuid The reference uuid of the existing {@link SchemaNode} to update
     * @deprecated Legacy function, use {@link #applyModification(String, DeltaModification)} instead
     */
    @Deprecated
    public void placeSchemaNode(@NotNull String modelId, @NotNull UUID schemaNodeUuid, @NotNull SchemaNode schemaNode) {
        if (!schemaNodeUuid.toString().equals(schemaNode.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed uuid in syntax model node is inconsistent to resource to update. "
                            + "Resource: " + schemaNodeUuid + " Payload: " + schemaNode.getUuid()
            );
        }
        if (schemaNode.getXCoordinate() == null || schemaNode.getYCoordinate() == null) {
            StringJoiner stringJoiner = new StringJoiner(" and ", "Missing ", " coordinate in SchemaNode.");
            if (schemaNode.getXCoordinate() == null) {
                stringJoiner.add("X");
            }
            if (schemaNode.getYCoordinate() == null) {
                stringJoiner.add("Y");
            }
            //noinspection HardcodedFileSeparator
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    stringJoiner.toString()
            );
        }

        updatePositions(modelId, Collections.singletonList(schemaNode));
    }

    /**
     * Apply a syntactic operation to the model.
     *
     * @param modelId        The data source to modify
     * @param syntacticOperationDTO The operation to invoke
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(String, DeltaModification)} instead
     */
    @Deprecated
    public @NotNull CombinedModel modifySyntaxModel(@NotNull String modelId, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        //load referenced data source
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);

        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        //deserialize syntactic operation
        Operation.Handle handle = schemaOperationDTOConverter.fromDTO(syntacticOperationDTO);

        final CombinedModel modifiedModel;
        try {
            modifiedModel = handle.invoke(combinedModel);
        } catch (ParameterParsingException e) {
            logger.warn("Failed to execute operation: {}", syntacticOperationDTO, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametrization for operation " + syntacticOperationDTO.getName() + " was invalid.");
        }

        persistCombinedModel(dataSourceModel, modifiedModel);
        return modifiedModel;
    }

    public void prepareForFrontend(CombinedModel combinedModel) {
        // add syntactic operations
        parameterDefinitionLookup = operationLookUp.getOperationHandles(combinedModel);

        combinedModel.getSyntaxModel().getNodes().forEach(schemaNode -> {
            schemaNode.getOperations().clear();
            schemaNode.getOperations().addAll(getOperationDTOs(schemaNode, parameterDefinitionLookup));
        });
    }

    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // .toList() never returns null, SB cant validate
    @NotNull
    private List<SyntacticOperationDTO> getOperationDTOs(@NotNull SchemaNode node, @NotNull HashMap<SchemaNode, Set<Operation.Handle>> parameterDefinitionLookup) {
        return parameterDefinitionLookup.getOrDefault(node, new HashSet<>()).stream()
                .map(handle -> new SyntacticOperationDTO(
                        handle.getOperation().getName(),
                        handle.getOperation().getLabel(),
                        handle.getOperation().getDescription(),
                        schemaOperationDTOConverter.toDTO(handle.getParameterDefinition())
                )).collect(Collectors.toList());
    }

    /**
     * Applies all modifications stored in the modification object to the {@link CombinedModel} for the data source
     * identified by modelId.
     *
     * @param modelId The data source uuid identifying the model
     * @param modification   The modifications. All modifications are applied in one step
     * @return The updated {@link CombinedModel}
     */
    @NotNull
    public CombinedModel applyModification(@NotNull String modelId, @NotNull DeltaModification modification) {

        //load schema
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        CombinedModel clone = combinedModel.copy();
        try {
            clone.apply(modification);
        } catch (Exception e) {
            // something went wrong
            // TODO add more fine grained exceptions
            logger.warn("Exception during application of modification: " + modification + ":");
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error during application of modification: " + e.getMessage());
        }
        persistCombinedModel(dataSourceModel, clone);

        return clone;
    }

    /**
     * This function updates the positions of the given movedElements.
     * All other changes are ignored, only the position information is updated.
     *
     * @param modelId The data source uuid identifying the {@link CombinedModel} to update
     * @param movedElements  A list of elements with updated positions, can be syntactic and semantic elements
     */
    public void updatePositions(@NotNull String modelId, @NotNull List<PositionedCombinedModelElement> movedElements) {
        //load referenced data source
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        // create a map for quick lookups
        final Map<String, PositionedCombinedModelElement> elementLookup =
                combinedModel.getSyntaxModel().getNodes().stream()
                        .map(schemaNode -> (PositionedCombinedModelElement) schemaNode)
                        .collect(Collectors.toMap(CombinedModelElement::getUuid, Function.identity()));

        combinedModel.getSemanticModel().getNodes().stream()
                .map(semanticNode -> (PositionedCombinedModelElement) semanticNode)
                .forEach(node -> elementLookup.put(node.getUuid(), node));

        for (PositionedCombinedModelElement movedElement : movedElements) {
            PositionedCombinedModelElement current = elementLookup.get(movedElement.getUuid());
            if (current == null) {
                logger.warn("Could not update position for non-existing element in CombinedModel: {}", movedElement.getUuid());
                continue;
            }
            current.setXCoordinate(movedElement.getXCoordinate());
            current.setYCoordinate(movedElement.getYCoordinate());
        }
        persistDataSourceModel(dataSourceModel);
    }


    @NotNull
    public SemanticModelNode cacheElement(@NotNull String modelId, @NotNull SemanticModelNode element) {

        element.validate();

        //load model - DON'T COPY since caching of transient concepts is not "undoable"
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        if (element instanceof Class || element instanceof NamedEntity) {
            final SemanticModelNode usedElement = getAndUpdateCache(
                    element,
                    SemanticModelNode::getURI,
                    combinedModel.getProvisionalElements());

            persistDataSourceModel(dataSourceModel);
            return usedElement;
        } else {
            logger.info("Received uncachable type " + element);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This type cannot be cached");
        }
    }

    public void uncacheElement(@NotNull String modelId, String uri) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalElements().removeIf(o -> o.getURI().equals(uri));
        if (change) {
            persistDataSourceModel(dataSourceModel);
        }
    }

    @NotNull
    public Relation cacheRelation(@NotNull String modelId, @NotNull Relation relation) {

        relation.convertToTemplate();
        relation.validate();

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);
        //compute entity concept
        final Relation usedRelation = getAndUpdateCache(
                relation,
                Relation::getUuid,
                combinedModel.getProvisionalRelations()
        );
        persistDataSourceModel(dataSourceModel);

        return usedRelation;

    }

    public void uncacheRelationConcept(@NotNull String modelId, String relationConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalRelations().removeIf(o -> o.getUuid().equals(relationConceptUuid));
        if (change) {
            persistDataSourceModel(dataSourceModel);
        }
    }

    /**
     * NOTE: this has side effects on the passed list
     * Finds the entry in the cache which is equivalent to {@code element} concerning it's identity function and returns it. If there is none it uses the
     * passed element and adds it to the cache. <i>Equivalence</i> of two elements {@code x} and {@code y} here means:
     * {@code identityFunction.apply(x).equals(identityFunction.apply(y)}
     *
     * @param element          the entry which is looked for
     * @param identityFunction the function which maps an element to it's {@link UUID} (usually just a method reference to T::getUuid)
     * @param elementCache     the cache of known entries
     * @return an entity concept which is equivalent to {@code entityConcept}
     */
    @NotNull
    private <T extends CombinedModelElement> T getAndUpdateCache(
            @NotNull T element,
            @NotNull Function<T, String> identityFunction,
            @NotNull List<T> elementCache
    ) {
        final T result;
        String uuid = identityFunction.apply(element);
        //if uuid exists see if one exists in the cache all-ready
        if (uuid != null) {
            Optional<T> cachedEntityConcept = elementCache.stream()
                    .filter(ec -> identityFunction.apply(ec) != null && uuid.equals(identityFunction.apply(ec)))
                    .findAny();
            result = cachedEntityConcept.orElse(element);
            if (cachedEntityConcept.isEmpty()) {
                elementCache.add(result);
            }
        } else {
            result = element;
            elementCache.add(result);
        }

        return result;
    }

    /* UNDO / REDO Functions */

    public @NotNull CombinedModel undoModelModification(@NotNull String modelId) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        try {
            dataSourceModel.popCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
        persistDataSourceModel(dataSourceModel);
        return dataSourceModel.getCurrentModel();
    }

    public @NotNull CombinedModel redoModelModification(@NotNull String modelId) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        try {
            dataSourceModel.restoreCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        persistDataSourceModel(dataSourceModel);
        return dataSourceModel.getCurrentModel();
    }

    /* Finish operations */

    /**
     * Finishes the modeling of the given data source.
     * The semantic model is persisted in the KGS.
     * Provisional concepts are dropped and recommendations removed.
     * After calling this function, the {@link CombinedModel} of the data source cannot be changed as it is set to finalized.
     * The given {@link CombinedModel} is cloned first, so the change is reversible as a new instance is returned.
     *
     * @param modelId The id of the data source to finish
     * @return An updated model instance with the new persistence ids (entityIds) included
     */
    @NotNull
    @Transactional
    public CombinedModel finishModeling(@NotNull String modelId) {
        //Load the data source from the repository.
        DataSourceModel dataSourceModel = dataSourceRepository.findById(UUID.fromString(modelId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));

        CombinedModel combinedModel = getCombinedModel(dataSourceModel, false);

        combinedModel.validate();

        try {
            combinedModel = createSemanticModel(combinedModel, modelId.toString());
        } catch (CombinedModelIntegrityException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred during conversion.");
        }
        // clear provisional elements and recommendations
        combinedModel.getRecommendations().clear();
        combinedModel.getProvisionalRelations().clear();
        combinedModel.getProvisionalElements().clear();
        combinedModel.getSyntaxModel().getNodes().forEach(schemaNode -> schemaNode.getOperations().clear());

        // prevent any further modifications
        combinedModel.finalizeModel();

        dataSourceModel.pushCombinedModel(combinedModel);
        DataSourceModel finalDataSource = dataSourceRepository.save(dataSourceModel);
        return finalDataSource.getCurrentModel();
    }

    /**
     * Sends the {@link SemanticModel} of the given {@link CombinedModel} to the KGS to persist it.
     * ID mappings (the KGS does not use our UUIDs) is done in this function, including the reverse mapping once
     * new KGS ids are generated. Those are then stored in the entityId field.
     * Equal concepts used by multiple entities or relations are merged during this process to not clutter the KG.
     * The given {@link CombinedModel} is cloned first, so the change is reversible as a new instance is returned.
     *
     * @param combinedModel  The combined model containing the {@link SemanticModel}
     * @param modelId The data source uuid
     * @return The updated {@link CombinedModel} with the new KGS ids assigned
     * @throws CombinedModelIntegrityException If the integrity of the {@link CombinedModel} is violated during the concept merge
     */
    @NotNull
    private CombinedModel createSemanticModel(@NotNull CombinedModel combinedModel, @NotNull String modelId) throws CombinedModelIntegrityException {

        combinedModel = combinedModel.copy();
        // merge the concepts to not create possible new concepts twice
        SemanticModel semanticModel = combinedModel.getSemanticModel();

        //Ensure that all instances have a unique name
        Map<String, SemanticModelNode> duplicateCheck = new HashMap<>();
        for (SemanticModelNode node : semanticModel.getNodes()) {
            if (duplicateCheck.containsKey(node.getLabel())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entity Type names must be unique");
            }
            duplicateCheck.put(node.getLabel(), node);
        }

        //send request
        final SemanticModel response;
        try {
            logger.info("Creating Semantic Model");
            response = semanticModelApiClient.createSemanticModel(semanticModel);
            logger.info("Created Semantic Model");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when creating semantic model in the KGS", e);
        }

        combinedModel = new CombinedModel(modelId, combinedModel.getSyntaxModel(), response, null, true);

        return combinedModel;
    }

    @Transactional
    public void deleteModel(@NotNull String modelId) {
        Optional<DataSourceModel> possibleSchema = dataSourceRepository.findById(UUID.fromString(modelId));
        if (possibleSchema.isPresent()) {
            dataSourceRepository.delete(possibleSchema.get());
        } else {
            logger.warn("Model with id {} was not available", modelId);
        }
    }

    @SuppressWarnings("unused")
    @Transactional
    void persistCombinedModel(@NotNull String modelId, @NotNull CombinedModel combinedModel) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(modelId);
        persistCombinedModel(dataSourceModel, combinedModel);
    }

    @Transactional
    void persistCombinedModel(@NotNull DataSourceModel dataSourceModel, @NotNull CombinedModel combinedModel) {
        dataSourceModel.pushCombinedModel(combinedModel);
        dataSourceRepository.save(dataSourceModel);
    }

    @Transactional
    void persistDataSourceModel(@NotNull DataSourceModel dataSourceModel) {
        dataSourceRepository.save(dataSourceModel);
    }


    @SuppressWarnings("SameParameterValue") // this might change
    @NotNull
    private CombinedModel getCombinedModel(@NotNull String modelId, boolean modelWillBeModified) {
        Optional<DataSourceModel> dataSourceModel = dataSourceRepository.findById(UUID.fromString(modelId));
        if (dataSourceModel.isPresent()) {
            try {
                return getCombinedModel(dataSourceModel.get(), modelWillBeModified);
            } catch (Exception e) {
                logger.warn("Could not get Combined Model in DMS, falling back to KGS:", e);
            }
        }
        //Check if a model is available in the KGS
        try {
            SemanticModel model = semanticModelApiClient.getSemanticModel(modelId);
            CombinedModel combinedModel = new CombinedModel(modelId, null, model);
            combinedModel.finalizeModel();
            return combinedModel;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Semantic Model was not found");
        }
    }

    @NotNull
    private CombinedModel getCombinedModel(@NotNull DataSourceModel dataSourceModel, boolean modelWillBeModified) {
        CombinedModel currentModel = dataSourceModel.getCurrentModel();

        if (modelWillBeModified && currentModel.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalized models are not allowed to be modified");
        }
        return currentModel;
    }


    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // throws Exception if null
    @NotNull
    private DataSourceModel findDataSourceOrThrow(@NotNull String modelId) {
        return dataSourceRepository.findById(UUID.fromString(modelId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data source uuid: " + modelId));
    }

    @NotNull
    public CombinedModel requestRecommendations(@NotNull String modelId, @NotNull CombinedModel combinedModel) {
        try {
            return semanticRecommendationApiClient.performSemanticRefinement(modelId, null, null, combinedModel);
        } catch (Exception e) {
            logger.warn("Could not reach SRS");
        }
        return combinedModel;
    }
}