package de.buw.tmdt.plasma.services.dms.core.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.dms.core.converter.KnowledgeGraphServiceDTO2CombinedModelConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.SchemaOperationDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.operations.Operation;
import de.buw.tmdt.plasma.services.dms.core.operations.OperationLookUp;
import de.buw.tmdt.plasma.services.dms.core.operations.exceptions.ParameterParsingException;
import de.buw.tmdt.plasma.services.dms.core.repository.DataSourceRepository;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.SemanticModelCreationResponse;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.LocalKnowledgeApiClient;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.UniversalKnowledgeApiClient;
import de.buw.tmdt.plasma.services.sas.shared.feignclient.SchemaAnalysisApiClient;
import de.buw.tmdt.plasma.services.srs.feignclient.SemanticRecommendationApiClient;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private final LocalKnowledgeApiClient localKnowledgeApiClient;
    private final UniversalKnowledgeApiClient universalKnowledgeApiClient;
    private final KnowledgeGraphServiceDTO2CombinedModelConverter kgs2CMConverter;
    private final DataSourceRepository dataSourceRepository;
    private final SchemaAnalysisApiClient schemaAnalysisApiClient;
    private final SemanticRecommendationApiClient semanticRecommendationApiClient;
    private final SchemaOperationDTOConverter schemaOperationDTOConverter;
    private final OperationLookUp operationLookUp;

    // TODO ensure this does not grow infinitely
    private final HashMap<String, CombinedModel> combinedModelCache = new HashMap<>();

    @Autowired
    public DataModelingHandler(
            @NotNull LocalKnowledgeApiClient localKnowledgeApiClient,
            @NotNull UniversalKnowledgeApiClient universalKnowledgeApiClient,
            @NotNull KnowledgeGraphServiceDTO2CombinedModelConverter kgs2CMConverter,
            @NotNull DataSourceRepository dataSourceRepository,
            @NotNull SchemaAnalysisApiClient schemaAnalysisApiClient,
            @NotNull SemanticRecommendationApiClient semanticRecommendationApiClient, SchemaOperationDTOConverter schemaOperationDTOConverter, OperationLookUp operationLookUp) {
        this.localKnowledgeApiClient = localKnowledgeApiClient;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        this.kgs2CMConverter = kgs2CMConverter;
        this.dataSourceRepository = dataSourceRepository;
        this.schemaAnalysisApiClient = schemaAnalysisApiClient;
        this.semanticRecommendationApiClient = semanticRecommendationApiClient;
        this.schemaOperationDTOConverter = schemaOperationDTOConverter;
        this.operationLookUp = operationLookUp;
    }

    public void initDataSourceSchema(@NotNull UUID uuid, int exampleLimit) {
        Optional<DataSourceModel> dataSourceResult = dataSourceRepository.findById(uuid);
        DataSourceModel dataSourceModel;
        if (dataSourceResult.isPresent()) {
            logger.info("Data source already exists - the current schema is replaced with the last existing one in the SAS");
            dataSourceModel = dataSourceResult.get();
            //dataSourceModel.setDataSourceSchemaStack(Collections.emptyList());
        } else {
            logger.info("Data source does not exist");
            DataSourceModel dsm = new DataSourceModel(uuid);
            dataSourceModel = dataSourceRepository.save(dsm);
            logger.info("Created a new data source");
        }
        dataSourceModel.pushCombinedModel(createInitialSchema(uuid, exampleLimit, dataSourceModel));

        //store the schema
        dataSourceRepository.save(dataSourceModel);
    }

    private CombinedModel createInitialSchema(UUID dataSourceUuid, int exampleLimit, DataSourceModel dataSourceModel) {
        @NotNull SyntaxModel syntaxModel;
        try {
            syntaxModel = schemaAnalysisApiClient.getResult(dataSourceUuid.toString(), exampleLimit);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach SchemaAnalysisService", e);
        }
        SemanticModel semanticModel;
        semanticModel = new SemanticModel(
                null,
                new ArrayList<>(),
                new ArrayList<>()
        );

        CombinedModel schema = new CombinedModel(
                dataSourceUuid.toString(),
                syntaxModel,
                semanticModel,
                new ArrayList<>(),
                new ArrayList<>(),
                false
        );

        dataSourceModel.pushCombinedModel(schema);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);

        // request semantic labeling
        CombinedModel combinedModel = dataSourceModel.getCurrentModel();
        combinedModel.getSyntaxModel().getNodes().forEach(node -> {
            // no operations when sending to other services
            node.getOperations().clear();
        });
        CombinedModel modifiedCombinedModel = semanticRecommendationApiClient.performSemanticLabeling(
                dataSourceUuid.toString(), combinedModel);

        // request semantic modeling
        modifiedCombinedModel = semanticRecommendationApiClient.performSemanticModeling(
                dataSourceUuid.toString(), modifiedCombinedModel);

        return modifiedCombinedModel;
    }

    public @NotNull CombinedModel getCombinedModel(@NotNull UUID dataSourceUuid) {
        return getCombinedModel(findDataSourceOrThrow(dataSourceUuid), false);
    }

    /**
     * Retrieves all available {@link EntityConcept}s, from the KGS as well as the current model
     *
     * @param dataSourceUuid The data source uuid
     * @param prefix         A search prefix
     * @return The resulting list of matching {@link EntityConcept}s
     */
    @NotNull
    public List<EntityConcept> getEntityConcepts(@NotNull UUID dataSourceUuid, @NotNull String prefix) {
        //Retrieve the concepts from the KGS
        ArrayList<EntityConcept> availableEntityConcepts;
        try {
            availableEntityConcepts = CollectionUtilities.map(
                    universalKnowledgeApiClient.getEntityConcepts(prefix),
                    kgs2CMConverter::convert,
                    ArrayList::new
            );
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach KnowledgeGraphService");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findDataSourceOrThrow(dataSourceUuid), false);
        combinedModel.getProvisionalEntityConcepts().stream()
                .filter(entityConcept -> entityConcept.getLabel().startsWith(prefix))
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(entityConcept -> availableEntityConcepts.stream().noneMatch(aec -> aec.sameAs(entityConcept)))
                .forEach(availableEntityConcepts::add);

        return availableEntityConcepts.stream()
                .sorted(Comparator.comparing(EntityConcept::getLabel))
                .collect(Collectors.toList());

    }

    @NotNull
    public List<RelationConcept> getRelationConcepts(@NotNull UUID dataSourceUuid, @NotNull String prefix) {
        // Retrieve the concepts from the KGS
        ArrayList<RelationConcept> availableRelationConcepts;
        try {
            availableRelationConcepts = CollectionUtilities.map(
                    universalKnowledgeApiClient.getRelationConcepts(prefix),
                    kgs2CMConverter::convert,
                    ArrayList::new
            );
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach KnowledgeGraphService");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findDataSourceOrThrow(dataSourceUuid), false);
        combinedModel.getProvisionalRelationConcepts().stream()
                .filter(relationConcept -> relationConcept.getLabel().startsWith(prefix))
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(relationConcept -> availableRelationConcepts.stream().noneMatch(arc -> arc.sameAs(relationConcept)))
                .forEach(availableRelationConcepts::add);

        return availableRelationConcepts.stream()
                .sorted(Comparator.comparing(RelationConcept::getLabel))
                .collect(Collectors.toList());
    }

    /* SYNTAX NODE OPERATIONS */

    /**
     * Updates the position of a {@link SchemaNode} in the {@link CombinedModel}.
     *
     * @param dataSourceUuid The data source uuid identifying the {@link CombinedModel}
     * @param schemaNode     The {@link SchemaNode} to place
     * @param schemaNodeUuid The reference uuid of the existing {@link SchemaNode} to update
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @Transactional
    @Deprecated
    public void placeSchemaNode(@NotNull UUID dataSourceUuid, @NotNull UUID schemaNodeUuid, @NotNull SchemaNode schemaNode) {
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

        updatePositions(dataSourceUuid, Collections.singletonList(schemaNode));
    }

    @Transactional
    public @NotNull CombinedModel modifySchema(@NotNull UUID uuid, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        //load referenced data source
        DataSourceModel dataSourceModel = findDataSourceOrThrow(uuid);

        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true); // TODO restore clone here?

        //deserialize syntactic operation
        Operation.Handle handle = schemaOperationDTOConverter.fromDTO(syntacticOperationDTO);

        final CombinedModel modifiedModel;
        try {
            modifiedModel = handle.invoke(combinedModel);
        } catch (ParameterParsingException e) {
            logger.warn("Failed to execute operation: {}", syntacticOperationDTO, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametrization for operation " + syntacticOperationDTO.getName() + " was invalid.");
        }

        //persist and return copy
        dataSourceModel.pushCombinedModel(modifiedModel);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.getCurrentModel();
    }

    /* SEMANTIC MODEL OPERATIONS */

    /**
     * Adds a new {@link EntityType} to the {@link CombinedModel}.
     *
     * @param dataSourceUuid    The data source uuid identifying the {@link CombinedModel}
     * @param entityType        The {@link EntityType} to add
     * @param primitiveNodeUuid The uuid of the {@link PrimitiveNode} this is to be applied.
     *                          If null, will not be applied to any syntax node.
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @Transactional
    @NotNull
    @Deprecated
    public CombinedModel addEntityType(@NotNull UUID dataSourceUuid, @NotNull EntityType entityType, @Nullable UUID primitiveNodeUuid) {

        if (primitiveNodeUuid != null) {
            entityType.setSyntaxNodeUuid(primitiveNodeUuid.toString());
        }

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);

        return applyModification(dataSourceUuid, modification);
    }

    /**
     * Updates an {@link EntityType} in the {@link CombinedModel}.
     *
     * @param dataSourceUuid       The data source uuid identifying the {@link CombinedModel}
     * @param entityType           The {@link EntityType} to add
     * @param entityTypeResourceId The reference uuid of the existing {@link EntityType} to update
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @NotNull
    @Transactional
    @Deprecated
    public CombinedModel updateEntityType(@NotNull UUID dataSourceUuid, @NotNull String entityTypeResourceId, @NotNull EntityType entityType) {
        if (!entityTypeResourceId.equals(entityType.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + entityTypeResourceId + " Payload: " + entityType.getUuid()
            );
        }

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);

        return applyModification(dataSourceUuid, modification);

    }

    /**
     * Updates the position of an {@link EntityType} in the {@link CombinedModel}.
     *
     * @param dataSourceUuid       The data source uuid identifying the {@link CombinedModel}
     * @param entityType           The {@link EntityType} to add
     * @param entityTypeResourceId The reference uuid of the existing {@link EntityType} to update
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @Transactional
    public void placeEntityType(@NotNull UUID dataSourceUuid, @NotNull String entityTypeResourceId, @NotNull EntityType entityType) {
        if (!entityTypeResourceId.equals(entityType.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + entityTypeResourceId + " Payload: " + entityType.getUuid()
            );
        }

        if (entityType.getXCoordinate() == null || entityType.getYCoordinate() == null) {
            StringJoiner stringJoiner = new StringJoiner(" and ", "Missing ", " coordinate in entity type.");
            if (entityType.getXCoordinate() == null) {
                stringJoiner.add("X");
            }
            if (entityType.getYCoordinate() == null) {
                stringJoiner.add("Y");
            }
            //noinspection HardcodedFileSeparator
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    stringJoiner.toString()
            );
        }
        updatePositions(dataSourceUuid, Collections.singletonList(entityType));
    }

    /**
     * Removes an {@link EntityType} from the {@link CombinedModel}.
     *
     * @param dataSourceUuid       The data source uuid identifying the {@link CombinedModel}
     * @param entityTypeResourceId The reference uuid of the existing {@link EntityType} to remove
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} with {@link DeltaModification#isDeletion()} instead
     */
    @NotNull
    @Transactional
    @Deprecated
    public CombinedModel removeEntityTypeFromSchema(@NotNull UUID dataSourceUuid, @NotNull String entityTypeResourceId) {

        EntityType entityType = new EntityType(entityTypeResourceId, null, "delete", null, "delete", "", null, null, null);

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);
        modification.setDeletion(true);

        return applyModification(dataSourceUuid, modification);
    }

    /**
     * Adds a new {@link Relation} to the {@link CombinedModel}.
     *
     * @param dataSourceUuid The data source uuid identifying the {@link CombinedModel}
     * @param relation       The {@link Relation} to add
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @NotNull
    @Transactional
    @Deprecated
    public CombinedModel addRelation(@NotNull UUID dataSourceUuid, @NotNull Relation relation) {

        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);

        return applyModification(dataSourceUuid, modification);
    }

    /**
     * Updates a {@link Relation} in the {@link CombinedModel}.
     *
     * @param dataSourceUuid     The data source uuid identifying the {@link CombinedModel}
     * @param relation           The {@link Relation} to update
     * @param relationResourceId The reference uuid of the existing {@link Relation} to update
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @NotNull
    @Transactional
    @Deprecated
    public CombinedModel updateRelation(@NotNull UUID dataSourceUuid, @NotNull String relationResourceId, @NotNull Relation relation) {
        if (!relationResourceId.equals(relation.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + relationResourceId + " Payload: " + relation.getUuid()
            );
        }
        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);
        return applyModification(dataSourceUuid, modification);
    }

    /**
     * Removes a {@link Relation} from the {@link CombinedModel}.
     *
     * @param dataSourceUuid     The data source uuid identifying the {@link CombinedModel}
     * @param relationResourceId The reference uuid of the existing {@link Relation} to remove
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} with {@link DeltaModification#isDeletion()} instead
     */
    @NotNull
    @Transactional
    public CombinedModel removeRelationFromSchema(@NotNull UUID dataSourceUuid, @NotNull String relationResourceId) {
        Relation relation = new Relation(relationResourceId, "delete", "delete", "delete", "", null);
        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);
        return applyModification(dataSourceUuid, modification);
    }

    /**
     * Applies all modifications stored in the modification object to the {@link CombinedModel} for the data source
     * identified by dataSourceUuid.
     *
     * @param dataSourceUuid The data source uuid identifying the model
     * @param modification   The modifications. All modifications are applied in one step
     * @return The updated {@link CombinedModel}
     */
    @NotNull
    @Transactional
    public CombinedModel applyModification(@NotNull UUID dataSourceUuid, @NotNull DeltaModification modification) {

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        combinedModel.apply(modification);

        // persist new state
        // TODO

        return combinedModel;
    }

    /**
     * This function updates the positions of the given movedElements.
     * All other changes are ignored, only the position information is updated.
     *
     * @param dataSourceUuid The data source uuid identifying the {@link CombinedModel} to update
     * @param movedElements  A list of elements with updated positions, can be syntactic and semantic elements
     */
    @Transactional
    public void updatePositions(@NotNull UUID dataSourceUuid, @NotNull List<PositionedCombinedModelElement> movedElements) {
        //load referenced data source
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
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

        dataSourceRepository.save(dataSourceModel);
    }

    /* CACHING OPERATIONS */

    @Transactional
    public EntityConcept cacheEntityConcept(@NotNull UUID dataSourceUuid, @NotNull EntityConcept entityConcept) {

        if (entityConcept.getEntityId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provisional entity concept must not have an entity id."
            );
        }

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        //compute entity concept
        final EntityConcept usedEntityConcept = getAndUpdateCache(
                entityConcept,
                EntityConcept::getUuid,
                combinedModel.getProvisionalEntityConcepts());

        dataSourceRepository.save(dataSourceModel);

        return usedEntityConcept;
    }

    @Transactional
    public void uncacheEntityConcept(@NotNull UUID dataSourceUuid, String entityConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalEntityConcepts().removeIf(o -> o.getUuid().equals(entityConceptUuid));
        if (change) {
            dataSourceRepository.save(dataSourceModel);
        }
    }

    @Transactional
    public RelationConcept cacheRelationConcept(@NotNull UUID dataSourceUuid, @NotNull RelationConcept relationConcept) {

        if (relationConcept.getEntityId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New relation concept must not have an entity id."
            );
        }

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);
        //compute entity concept
        final RelationConcept usedRelationConcept = getAndUpdateCache(
                relationConcept,
                RelationConcept::getUuid,
                combinedModel.getProvisionalRelationConcepts()
        );

        dataSourceRepository.save(dataSourceModel);

        return usedRelationConcept;

    }

    @Transactional
    public void uncacheRelationConcept(@NotNull UUID dataSourceUuid, String relationConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalRelationConcepts().removeIf(o -> o.getUuid().equals(relationConceptUuid));
        if (change) {
            dataSourceRepository.save(dataSourceModel);
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

    @Transactional
    public @NotNull CombinedModel undoModelModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));
        try {
            dataSourceModel.popCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.getCurrentModel();
    }

    @Transactional
    public @NotNull CombinedModel redoModelModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));
        CombinedModel model;
        try {
            model = dataSourceModel.restoreCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        dataSourceRepository.save(dataSourceModel);
        return model;
    }

    /* Finish operations */

    @NotNull
    public CombinedModel finishModeling(@NotNull UUID dataSourceUuid) {
        //Load the data source from the repository.
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));

        final CombinedModel combinedModel = getCombinedModel(dataSourceModel, false);

        combinedModel.validate();

        final SemanticModel semanticModel;
        try {
            semanticModel = createSemanticModel(combinedModel, dataSourceUuid.toString());
        } catch (CombinedModelIntegrityException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred during conversion.");
        }
        // TODO use altered semantic model here!
        // prevent any further modifications
        combinedModel.finalizeModel();

        dataSourceModel.pushCombinedModel(combinedModel);
        DataSourceModel finalDataSource = dataSourceRepository.save(dataSourceModel);
        return finalDataSource.getCurrentModel();
    }

    @NotNull
    private SemanticModel createSemanticModel(@NotNull CombinedModel combinedModel, @NotNull String dataSourceUuid) throws CombinedModelIntegrityException {
        // merge the concepts to not create possible new concepts twice
        // TODO do a clone here?
        SemanticModel semanticModel = combinedModel.getSemanticModel().mergeConcepts();

        //create request
        final SemanticModelDTO semanticModelDTO = kgs2CMConverter.convert(semanticModel, dataSourceUuid);

        //send request
        final SemanticModelCreationResponse semanticModelCreationResponse;
        try {
            logger.info("Creating Semantic Model");
            semanticModelCreationResponse = localKnowledgeApiClient.createSemanticModel(semanticModelDTO);
            logger.info("Created Semantic Model");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when creating semantic model in the KGS", e);
        }

        // parse response and get id mapping
        // id mapping links KGS ids of newly created elements to the original given UUIDs from the original CombinedModel
        Map<String, String> kgsIdToUuidMapping = semanticModelCreationResponse.getOldToNewIdLookup();
        // currently unused as the coverter maps the KGS Ids to the entityId field by default.
        // New element UUIDs are created anyway during that step.

        final SemanticModel convertedModel = kgs2CMConverter.convert(semanticModelCreationResponse.getSemanticModelDTO());

        return convertedModel;
    }

    /* Probably unused, needs to be adjusted to new data model if still needed or deleted if not */

//    @NotNull
//    private DataSourceSchema createFinalDataSourceSchema(
//            @NotNull DataSourceSchema currentDataSourceSchema,
//            @NotNull SemanticModel retrievedSemanticModel,
//            @NotNull Map<Long, String> modeling2KGSmapping
//    ) {
//        Map<Traversable.Identity<?>, Traversable> oldIdentityToNewTraversable = new HashMap<>();
//        final Node newSyntaxModel = currentDataSourceSchema.getSyntaxModel().copy(oldIdentityToNewTraversable);
//
//        //find all entity types which are mapped by a primitive node
//        final Set<EntityType> mappedEntityTypes = oldIdentityToNewTraversable.values().stream()
//                .filter(PrimitiveNode.class::isInstance)
//                .map(PrimitiveNode.class::cast)
//                .filter(p -> p.getEntityType() != null)
//                .map(PrimitiveNode::getEntityType)
//                .collect(Collectors.toSet());
//
//        for (EntityType mappedEntityType : mappedEntityTypes) {
//            final Traversable.Identity<?> oldEntityTypeIdentity = MapUtilities.keyOfValue(oldIdentityToNewTraversable, mappedEntityType);
//            final Traversable traversable = currentDataSourceSchema.find(oldEntityTypeIdentity);
//            assert traversable instanceof EntityType;
//            EntityType oldEntityType = (EntityType) traversable;
//            final String actualEntityTypeUuid = modeling2KGSmapping.get(oldEntityType.getId());
//
//            EntityType replacement = null;
//            for (EntityType entityType : retrievedSemanticModel.getEntityTypes()) {
//                if (actualEntityTypeUuid.equals(entityType.getUuid())) {
//                    replacement = entityType;
//                    break;
//                }
//            }
//            if (replacement == null) {
//                logger.warn(
//                        "Defective response for semantic model creation.\n" + "Mapping: {}\n" + "SemanticModel: {}",
//                        modeling2KGSmapping,
//                        retrievedSemanticModel
//                );
//                throw new RuntimeException(
//                        "Failed to find entity type for id `" + oldEntityType.getId() + "` which should be mapped to `" + actualEntityTypeUuid + "`."
//                );
//            }
//            newSyntaxModel.replace(mappedEntityType.getIdentity(), replacement);
//        }
//
//        //Update positions
//        //Create a map between all old entity types and their new backend ID
//        Map<String, EntityType> backendIDtoOldEntityTypeMapping =
//                currentDataSourceSchema.getSemanticModel().getEntityTypes().stream().collect(Collectors.toMap(
//                        x -> modeling2KGSmapping.get(x.getId()),
//                        Function.identity()
//                ));
//
//        //Update positions
//        for (EntityType entityType : retrievedSemanticModel.getEntityTypes()) {
//            entityType.setPosition(backendIDtoOldEntityTypeMapping.get(entityType.getUuid()).getPosition());
//        }
//
//        DataSourceSchema finalSchema = new DataSourceSchema(
//                currentDataSourceSchema.getOwningNode(),
//                newSyntaxModel,
//                retrievedSemanticModel,
//                Collections.emptyList(),
//                Collections.emptyList()
//        );
//        finalSchema.setFinalized(true);
//        return finalSchema;
//    }

    @Transactional
    public void deleteModel(@NotNull UUID dataSourceUuid) {
        Optional<DataSourceModel> possibleSchema = dataSourceRepository.findById(dataSourceUuid);
        if (possibleSchema.isPresent()) {
            dataSourceRepository.delete(possibleSchema.get());
        } else {
            logger.warn("Model with id {} was not available", dataSourceUuid);
        }
    }


    /*
     Utility functions
     */

    // CURRENTLY UNUSED, may be restored later
//    /**
//     * Loads a ModelBase element from the given repository and validates if the loaded element's UUID matches the given element's UUID in relation to the
//     * passed globalIdFunction.
//     *
//     * @param element       the element to load
//     * @param uuidGenerator the function to determine a global UUID from the given elements type
//     * @param repository    the repository to load from
//     * @return attached element matching the given transient element
//     */
//    @NotNull
//    private <T extends ModelBase> T loadFromRepository(
//            @NotNull T element,
//            @NotNull Function<T, String> uuidGenerator,
//            @NotNull JpaRepository<T, Long> repository
//    ) {
//        String uuid = uuidGenerator.apply(element);
//        Long longId = element.getId();
//        final T result = repository.findById(longId)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.BAD_REQUEST,
//                        "Not a valid " + element.getClass().getSimpleName() + " id: " + longId
//                ));
//
//        if (uuid != null && !uuid.equals(uuidGenerator.apply(result))) {
//            //noinspection MagicCharacter
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Inconsistent " + element.getClass().getSimpleName() + " received: id and uuid did not match data base state."
//            );
//        }
//        return result;
//    }

    @Transactional
    void persistCombinedModel(@NotNull UUID dataSourceUuid, @NotNull CombinedModel combinedModel) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        dataSourceModel.pushCombinedModel(combinedModel);
        dataSourceRepository.save(dataSourceModel);
    }


    @NotNull
    private CombinedModel getCombinedModel(@NotNull UUID dataSourceUuid, boolean modelWillBeModified) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        return getCombinedModel(dataSourceModel, modelWillBeModified);
    }

    @NotNull
    private CombinedModel getCombinedModel(@NotNull DataSourceModel dataSourceModel, boolean modelWillBeModified) {
        CombinedModel currentModel;
        if (!combinedModelCache.containsKey(dataSourceModel.getUuid().toString())) {
            currentModel = dataSourceModel.getCurrentModel();
            combinedModelCache.put(dataSourceModel.getUuid().toString(), currentModel);
        } else {
            // use the cached version, no need to reload from database
            currentModel = combinedModelCache.get(dataSourceModel.getUuid().toString());
        }
        if (modelWillBeModified && currentModel.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalized models are not allowed to be modified");
        }
        return currentModel;
    }


    @NotNull
    private DataSourceModel findDataSourceOrThrow(@NotNull UUID dataSourceUuid) {
        return dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data source uuid " + dataSourceUuid + ". "));
    }

    public CombinedModel requestRecommendations(String dataSourceUuid, CombinedModel combinedModel) {
        return null;
    }
}