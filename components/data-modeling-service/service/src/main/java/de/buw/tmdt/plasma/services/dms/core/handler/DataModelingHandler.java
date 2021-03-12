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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private HashMap<SchemaNode, Set<Operation.Handle>> parameterDefinitionLookup;

    @Autowired
    public DataModelingHandler(
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull LocalKnowledgeApiClient localKnowledgeApiClient,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull UniversalKnowledgeApiClient universalKnowledgeApiClient,
            @NotNull KnowledgeGraphServiceDTO2CombinedModelConverter kgs2CMConverter,
            @NotNull DataSourceRepository dataSourceRepository,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull SchemaAnalysisApiClient schemaAnalysisApiClient,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            @NotNull SemanticRecommendationApiClient semanticRecommendationApiClient,
            @NotNull SchemaOperationDTOConverter schemaOperationDTOConverter,
            @NotNull OperationLookUp operationLookUp) {
        this.localKnowledgeApiClient = localKnowledgeApiClient;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        this.kgs2CMConverter = kgs2CMConverter;
        this.dataSourceRepository = dataSourceRepository;
        this.schemaAnalysisApiClient = schemaAnalysisApiClient;
        this.semanticRecommendationApiClient = semanticRecommendationApiClient;
        this.schemaOperationDTOConverter = schemaOperationDTOConverter;
        this.operationLookUp = operationLookUp;
    }

    public void initDataSourceSchema(@NotNull UUID dataSourceUuid, int exampleLimit) {
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid).orElse(null);
        if (dataSourceModel != null) {
            logger.info("Received init request for already existing data source, skipping...");
            return;
        }
        Optional<DataSourceModel> dataSourceResult = dataSourceRepository.findById(dataSourceUuid);

        if (dataSourceResult.isPresent()) {
            logger.info("Data source already exists - the current schema is replaced with the last existing one in the SAS");
            dataSourceModel = dataSourceResult.get();
            //dataSourceModel.setDataSourceSchemaStack(Collections.emptyList());
        } else {
            logger.info("Data source does not exist");
            DataSourceModel dsm = new DataSourceModel(dataSourceUuid);
            dataSourceModel = dataSourceRepository.save(dsm);
            logger.info("Created a new data source");
        }
        dataSourceModel.pushCombinedModel(createInitialSchema(dataSourceUuid, exampleLimit, dataSourceModel));

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

        CombinedModel syntaxCombinedModel = new CombinedModel(
                dataSourceUuid.toString(),
                syntaxModel,
                semanticModel,
                new ArrayList<>(),
                new ArrayList<>(),
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
                    dataSourceUuid.toString(), combinedModel);

            try {
                CombinedModel modeledCombinedModel;
                // request semantic modeling
                modeledCombinedModel = semanticRecommendationApiClient.performSemanticModeling(
                        dataSourceUuid.toString(), labeledCombinedModel);
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

    public @NotNull CombinedModel getCombinedModel(@NotNull UUID dataSourceUuid) {

        CombinedModel combinedModel = getCombinedModel(dataSourceUuid, false);
        prepareForFrontend(combinedModel);
        return combinedModel;
    }

    /**
     * Retrieves all available {@link EntityConcept}s, from the KGS as well as the current model.
     *
     * @param dataSourceUuid The data source uuid
     * @param prefix         A search prefix
     * @return The resulting list of matching {@link EntityConcept}s
     */
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
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

    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
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

    /**
     * Apply a syntactic operation to the model.
     *
     * @param dataSourceUuid        The data source to modify
     * @param syntacticOperationDTO The operation to invoke
     * @return The updated model
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @Deprecated
    public @NotNull CombinedModel modifySyntaxModel(@NotNull UUID dataSourceUuid, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
        //load referenced data source
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);

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
        prepareForFrontend(modifiedModel);
        return modifiedModel;
    }

    private void prepareForFrontend(CombinedModel combinedModel) {
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
    @NotNull
    @Deprecated
    public CombinedModel addEntityType(@NotNull UUID dataSourceUuid, @NotNull EntityType entityType, @Nullable String primitiveNodeUuid) {
        if (primitiveNodeUuid != null) {
            entityType.setSyntaxNodeUuid(primitiveNodeUuid);
        }

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);

        CombinedModel combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;
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
    @Deprecated
    public CombinedModel updateEntityType(@NotNull UUID dataSourceUuid, @NotNull String entityTypeResourceId, @NotNull EntityType entityType) {
        if (!entityTypeResourceId.equals(entityType.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + entityTypeResourceId + " Payload: " + entityType.getUuid()
            );
        }

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);

        CombinedModel combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;

    }

    /**
     * Updates the position of an {@link EntityType} in the {@link CombinedModel}.
     *
     * @param dataSourceUuid       The data source uuid identifying the {@link CombinedModel}
     * @param entityType           The {@link EntityType} to add
     * @param entityTypeResourceId The reference uuid of the existing {@link EntityType} to update
     * @deprecated Legacy function, use {@link #applyModification(UUID, DeltaModification)} instead
     */
    @Deprecated
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
    @Deprecated
    public CombinedModel removeEntityTypeFromModel(@NotNull UUID dataSourceUuid, @NotNull String entityTypeResourceId) {

        EntityType entityType = getCombinedModel(dataSourceUuid, false).getSemanticModel().getEntityType(entityTypeResourceId);

        DeltaModification modification = new DeltaModification("internal_mod", Collections.singleton(entityType), null, null, null, 0.0);
        modification.setDeletion(true);

        CombinedModel combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;
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
    @Deprecated
    public CombinedModel addRelation(@NotNull UUID dataSourceUuid, @NotNull Relation relation) {

        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);

        CombinedModel combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;
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
    @Deprecated
    public CombinedModel updateRelation(@NotNull UUID dataSourceUuid, @NotNull String relationResourceId, @NotNull Relation relation) {
        if (!relationResourceId.equals(relation.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + relationResourceId + " Payload: " + relation.getUuid()
            );
        }
        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);
        CombinedModel combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;
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
    public CombinedModel removeRelationFromModel(@NotNull UUID dataSourceUuid, @NotNull String relationResourceId) {
        CombinedModel combinedModel = getCombinedModel(dataSourceUuid);
        Relation relation = combinedModel.getSemanticModel().getRelation(relationResourceId);
        DeltaModification modification = new DeltaModification("internal_mod", null, Collections.singleton(relation), null, null, 0.0);
        modification.setDeletion(true);
        combinedModel = applyModification(dataSourceUuid, modification);
        prepareForFrontend(combinedModel);
        return combinedModel;
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
    public CombinedModel applyModification(@NotNull UUID dataSourceUuid, @NotNull DeltaModification modification) {

        //load schema
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
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

        prepareForFrontend(clone);
        return clone;
    }

    /**
     * This function updates the positions of the given movedElements.
     * All other changes are ignored, only the position information is updated.
     *
     * @param dataSourceUuid The data source uuid identifying the {@link CombinedModel} to update
     * @param movedElements  A list of elements with updated positions, can be syntactic and semantic elements
     */
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
        persistDataSourceModel(dataSourceModel);
    }

    /* CACHING OPERATIONS */

    @NotNull
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


        persistDataSourceModel(dataSourceModel);

        return usedEntityConcept;
    }

    public void uncacheEntityConcept(@NotNull UUID dataSourceUuid, String entityConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalEntityConcepts().removeIf(o -> o.getUuid().equals(entityConceptUuid));
        if (change) {
            persistDataSourceModel(dataSourceModel);
        }
    }

    @NotNull
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
        persistDataSourceModel(dataSourceModel);

        return usedRelationConcept;

    }

    public void uncacheRelationConcept(@NotNull UUID dataSourceUuid, String relationConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = getCombinedModel(dataSourceModel, true);

        boolean change = combinedModel.getProvisionalRelationConcepts().removeIf(o -> o.getUuid().equals(relationConceptUuid));
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

    public @NotNull CombinedModel undoModelModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        try {
            dataSourceModel.popCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
        persistDataSourceModel(dataSourceModel);
        CombinedModel combinedModel = dataSourceModel.getCurrentModel();
        prepareForFrontend(combinedModel);
        return combinedModel;
    }

    public @NotNull CombinedModel redoModelModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        try {
            dataSourceModel.restoreCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        persistDataSourceModel(dataSourceModel);
        CombinedModel combinedModel = dataSourceModel.getCurrentModel();
        prepareForFrontend(combinedModel);
        return combinedModel;
    }

    /* Finish operations */

    /**
     * Finishes the modeling of the given data source.
     * The semantic model is persisted in the KGS.
     * Provisional concepts are dropped and recommendations removed.
     * After calling this function, the {@link CombinedModel} of the data source cannot be changed as it is set to finalized.
     * The given {@link CombinedModel} is cloned first, so the change is reversible as a new instance is returned.
     *
     * @param dataSourceUuid The id of the data source to finish
     * @return An updated model instance with the new persistence ids (entityIds) included
     */
    @NotNull
    @Transactional
    public CombinedModel finishModeling(@NotNull UUID dataSourceUuid) {
        //Load the data source from the repository.
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));

        CombinedModel combinedModel = getCombinedModel(dataSourceModel, false);

        combinedModel.validate();

        try {
            combinedModel = createSemanticModel(combinedModel, dataSourceUuid.toString());
        } catch (CombinedModelIntegrityException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred during conversion.");
        }
        // clear provisional elements and recommendations
        combinedModel.getRecommendations().clear();
        combinedModel.getProvisionalRelationConcepts().clear();
        combinedModel.getProvisionalEntityConcepts().clear();
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
     * @param dataSourceUuid The data source uuid
     * @return The updated {@link CombinedModel} with the new KGS ids assigned
     * @throws CombinedModelIntegrityException If the integrity of the {@link CombinedModel} is violated during the concept merge
     */
    @NotNull
    private CombinedModel createSemanticModel(@NotNull CombinedModel combinedModel, @NotNull String dataSourceUuid) throws CombinedModelIntegrityException {

        combinedModel = combinedModel.copy();
        // merge the concepts to not create possible new concepts twice
        SemanticModel semanticModel = combinedModel.getSemanticModel();
        semanticModel = semanticModel.mergeConcepts();

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

        // add the newly created entityIds to their respective elements
        combinedModel.getSemanticModel().getNodes().stream()
                .filter(entityType -> entityType.getEntityId() == null)
                .forEach(element -> {
                            String entityId = kgsIdToUuidMapping.get(element.getUuid());
                            if (entityId != null) {
                                // new entityId was created for that element
                                element.setEntityId(entityId);
                            }
                            if (element.getEntityConcept().getEntityId() == null) {
                                entityId = kgsIdToUuidMapping.get(element.getEntityConcept().getUuid());
                                if (entityId != null) {
                                    // new entityId was created for that element
                                    element.getEntityConcept().setEntityId(entityId);
                                }
                            }
                        }
                );

        combinedModel.getSemanticModel().getEdges().stream()
                .filter(entityType -> entityType.getEntityId() == null)
                .forEach(element -> {
                            String entityId = kgsIdToUuidMapping.get(element.getUuid());
                            if (entityId != null) {
                                // new entityId was created for that element
                                element.setEntityId(entityId);
                            }
                            if (element.getConcept().getEntityId() == null) {
                                entityId = kgsIdToUuidMapping.get(element.getConcept().getUuid());
                                if (entityId != null) {
                                    // new entityId was created for that element
                                    element.getConcept().setEntityId(entityId);
                                }
                            }
                        }
                );

        // dont forget the semantic model
        // we use the just generated UUID from the DTO here as the original SM had no UUID
        combinedModel.getSemanticModel().setEntityId(kgsIdToUuidMapping.get(semanticModelDTO.getId()));

        return combinedModel;
    }

    /* Probably unused, needs to be adjusted to new data model if still needed or deleted if not
     * May be needed for compatibility to other components*/


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

    @SuppressWarnings("unused")
    @Transactional
    void persistCombinedModel(@NotNull UUID dataSourceUuid, @NotNull CombinedModel combinedModel) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
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
    private CombinedModel getCombinedModel(@NotNull UUID dataSourceUuid, boolean modelWillBeModified) {
        Optional<DataSourceModel> dataSourceModel = dataSourceRepository.findById(dataSourceUuid);
        if (dataSourceModel.isPresent()) {
            try {
                return getCombinedModel(dataSourceModel.get(), modelWillBeModified);
            } catch (Exception e) {
                logger.warn("Could not get Combined Model in DMS, falling back to KGS:", e);
            }
        }
        //Check if a model is available in the KGS
        try {
            SemanticModelDTO semanticModelByDataSourceId = localKnowledgeApiClient.findSemanticModelByDataSourceId(dataSourceUuid.toString());
            SemanticModel model = kgs2CMConverter.convert(semanticModelByDataSourceId);
            CombinedModel combinedModel = new CombinedModel(dataSourceUuid.toString(), null, model);
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
    private DataSourceModel findDataSourceOrThrow(@NotNull UUID dataSourceUuid) {
        return dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data source uuid: " + dataSourceUuid));
    }

    @NotNull
    public CombinedModel requestRecommendations(@NotNull String dataSourceUuid, @NotNull CombinedModel combinedModel) {
        return combinedModel;
    }
}