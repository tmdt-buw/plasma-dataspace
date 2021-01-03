package de.buw.tmdt.plasma.services.dms.core.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.services.dms.core.converter.CombinedModelConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.KnowledgeGraphServiceDTO2CombinedModelConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.SchemaAnalysisDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.SchemaOperationDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.model.Position;
import de.buw.tmdt.plasma.services.dms.core.model.Traversable;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceSchema;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.Relation;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.Node;
import de.buw.tmdt.plasma.services.dms.core.operations.impl.SetEntityType;
import de.buw.tmdt.plasma.services.dms.core.repository.*;
import de.buw.tmdt.plasma.services.dms.core.services.DataSourceModelInitService;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.SemanticModelCreationResponse;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.LocalKnowledgeApiClient;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.UniversalKnowledgeApiClient;
import de.buw.tmdt.plasma.services.srs.feignclient.SemanticRecommendationApiClient;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.collections.MapUtilities;
import de.buw.tmdt.plasma.utilities.misc.ObjectUtilities;
import de.buw.tmdt.plasma.utilities.misc.Pair;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
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
    private final SchemaOperationDTOConverter schemaOperationDTOConverter;
    private final KnowledgeGraphServiceDTO2CombinedModelConverter knowledgeGraphServiceDTO2ModelConverter;
    private final KnowledgeGraphServiceDTO2CombinedModelConverter kgs2CMConverter;
    private final DataSourceRepository dataSourceRepository;
    private final SchemaNodeRepository schemaNodeRepository;
    private final EntityConceptRepository entityConceptRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final RelationConceptRepository relationConceptRepository;
    private final DataSourceModelInitService dataSourceModelInitService;
    private final SemanticRecommendationApiClient semanticRecommendationApiClient;
    private final SchemaAnalysisDTOConverter schemaAnalysisDTOConverter;
    private final CombinedModelConverter combinedModelConverter;

    @Autowired
    public DataModelingHandler(
            @NotNull LocalKnowledgeApiClient localKnowledgeApiClient,
            @NotNull UniversalKnowledgeApiClient universalKnowledgeApiClient,
            @NotNull SchemaOperationDTOConverter schemaOperationDTOConverter,
            @NotNull KnowledgeGraphServiceDTO2CombinedModelConverter knowledgeGraphServiceDTO2ModelConverter,
            @NotNull KnowledgeGraphServiceDTO2CombinedModelConverter kgs2CMConverter,
            @NotNull DataSourceRepository dataSourceRepository,
            @NotNull SchemaNodeRepository schemaNodeRepository,
            @NotNull EntityConceptRepository entityConceptRepository,
            @NotNull EntityTypeRepository entityTypeRepository,
            @NotNull RelationConceptRepository relationConceptRepository,
            @NotNull DataSourceModelInitService dataSourceModelInitService,
            SemanticRecommendationApiClient semanticRecommendationApiClient,
            SchemaAnalysisDTOConverter schemaAnalysisDTOConverter,
            CombinedModelConverter combinedModelConverter) {
        this.localKnowledgeApiClient = localKnowledgeApiClient;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        this.schemaOperationDTOConverter = schemaOperationDTOConverter;
        this.combinedModelConverter = combinedModelConverter;
        this.knowledgeGraphServiceDTO2ModelConverter = knowledgeGraphServiceDTO2ModelConverter;
        this.kgs2CMConverter = kgs2CMConverter;
        this.dataSourceRepository = dataSourceRepository;
        this.schemaNodeRepository = schemaNodeRepository;
        this.entityConceptRepository = entityConceptRepository;
        this.entityTypeRepository = entityTypeRepository;
        this.relationConceptRepository = relationConceptRepository;
        this.dataSourceModelInitService = dataSourceModelInitService;
        this.semanticRecommendationApiClient = semanticRecommendationApiClient;
        this.schemaAnalysisDTOConverter = schemaAnalysisDTOConverter;
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
            dataSourceModel = dataSourceRepository.saveAndFlush(dsm);
            logger.info("Created a new data source");
        }
        dataSourceModel.pushDataSourceSchema(dataSourceModelInitService.createInitialSchema(uuid, exampleLimit, dataSourceModel));

        //store the schema
        dataSourceRepository.save(dataSourceModel);
    }

    @NotNull
    public DataSourceSchema getDataSourceSchema(@NotNull UUID dataSourceUuid) {
        return peekDataSourceSchema(findDataSourceOrThrow(dataSourceUuid), false);
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
        CombinedModel schema = peekDataSourceSchema(findDataSourceOrThrow(dataSourceUuid), false);
        schema.getSemanticModel().getProvisionalEntityConcepts().stream()
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
        CombinedModel schema = peekDataSourceSchema(findDataSourceOrThrow(dataSourceUuid), false);
        schema.getSemanticModel().getProvisionalRelationConcepts().stream()
                .filter(relationConcept -> relationConcept.getLabel().startsWith(prefix))
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(relationConcept -> availableRelationConcepts.stream().noneMatch(arc -> arc.sameAs(relationConcept)))
                .forEach(availableRelationConcepts::add);

        return availableRelationConcepts.stream()
                .sorted(Comparator.comparing(RelationConcept::getLabel))
                .collect(Collectors.toList());
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
        CombinedModel combinedModel = peekDataSourceSchema(dataSourceModel, true);

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

    @NotNull
    @Transactional
    public DataSourceSchema modifySchema(@NotNull UUID uuid, @NotNull SyntacticOperationDTO syntacticOperationDTO) {
//        //load referenced data source
//        DataSourceModel dataSourceModel = findDataSourceOrThrow(uuid);
//        DataSourceSchema loadedSchema = peekDataSourceSchema(dataSourceModel, true);
//        DataSourceSchema newSchema = loadedSchema.copy();
//
//        //deserialize syntactic operation
//        Operation.Handle handle = schemaOperationDTOConverter.fromDTO(syntacticOperationDTO);
//
//        final DataSourceSchema modifiedSchema;
//        try {
//            modifiedSchema = handle.invoke(newSchema);
//        } catch (ParameterParsingException e) {
//            logger.warn("Failed to execute operation: {}", syntacticOperationDTO, e);
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametrization for operation " + syntacticOperationDTO.getName() + " was invalid.");
//        }
//
//        //persist and return copy
//        dataSourceModel.pushDataSourceSchema(modifiedSchema);
//        dataSourceModel = dataSourceRepository.save(dataSourceModel);
//        return dataSourceModel.peekDataSourceSchema();
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Currently not available.");
    }

    @Transactional
    @NotNull
    public DataSourceSchema addEntityType(@NotNull UUID dataSourceUuid, @NotNull EntityTypeDTO entityTypeDTO, @Nullable UUID primitiveNodeUuid) {
        //deserialize
        de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType receivedEntityType = semanticModelDTOConverter.fromDTO(entityTypeDTO);

        if (receivedEntityType.getId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New entity type must not have an id."
            );
        }

        //load
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        //compute entity concept
        final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept usedEntityConcept;
        if (receivedEntityType.getEntityConcept().getId() != null) {
            usedEntityConcept = loadFromRepository(receivedEntityType.getEntityConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept::getUuid, entityConceptRepository);
        } else {
            usedEntityConcept = getAndUpdateCache(receivedEntityType.getEntityConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept::getUuid, newSchema.getEntityConceptCache());
        }

        //update model
        receivedEntityType.setEntityConcept(usedEntityConcept);
        newSchema.getSemanticModel().getEntityTypes().add(receivedEntityType);

        //link entity type to primitive node if one [primitive node] was provided
        if (primitiveNodeUuid != null) {
            Traversable traversable = newSchema.getSyntaxModel().find(new Traversable.Identity<>(primitiveNodeUuid));
            if (!(traversable instanceof de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.PrimitiveNode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Referenced syntax model node was not of type `" + de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.PrimitiveNode.class.getSimpleName()
                                + "` but was " + (traversable != null ? traversable.getClass().getSimpleName() : null)
                );
            }
            de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.PrimitiveNode primitiveNode = (de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.PrimitiveNode) traversable;
            final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType oldEntityType = primitiveNode.getEntityType();
            if (oldEntityType != null) {
                newSchema.remove(oldEntityType.getIdentity());
            }
            primitiveNode.setEntityType(receivedEntityType);
            if (primitiveNode.getDataType() == DataType.UNKNOWN) {
                // if data type is still unknown, try to determine one
                primitiveNode.setDataType(SetEntityType.determineDataType(primitiveNode.getExamples()));
            }
            receivedEntityType.setPosition(primitiveNode.getPosition());
        }
        dataSourceModel.pushDataSourceSchema(newSchema);
        //persist and return copy
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @NotNull
    @Transactional
    public DataSourceSchema updateEntityType(@NotNull UUID dataSourceUuid, @NotNull Long entityTypeResourceId, @NotNull EntityTypeDTO entityTypeDTO) {
        final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType retrievedEntityType = semanticModelDTOConverter.fromDTO(entityTypeDTO);
        if (!entityTypeResourceId.equals(entityTypeDTO.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + entityTypeResourceId + " Payload: " + entityTypeDTO.getId()
            );
        }

        if (retrievedEntityType.getEntityConcept().getId() == null && retrievedEntityType.getEntityConcept().getUuid() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Creating new entity concepts is not supported on updating an entity type. Either set id or uuid of referenced entity concept."
            );
        }

        //load schema copy
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        final DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept entityConcept = loadFromRepository(retrievedEntityType.getEntityConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept::getUuid, entityConceptRepository);
        retrievedEntityType.setEntityConcept(entityConcept);
        de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType newEntityType = retrievedEntityType.copy();
        newSchema.replace(retrievedEntityType.getIdentity(), newEntityType);
        dataSourceModel.pushDataSourceSchema(newSchema);

        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @Transactional
    public void placeEntityType(@NotNull UUID dataSourceUuid, @NotNull Long entityTypeResourceId, @NotNull EntityTypeDTO entityTypeDTO) {
        if (!entityTypeResourceId.equals(entityTypeDTO.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in entity type is inconsistent to resource to update. Resource: " + entityTypeResourceId + " Payload: " + entityTypeDTO.getId()
            );
        }
        if (entityTypeDTO.getXCoordinate() == null || entityTypeDTO.getYCoordinate() == null) {
            StringJoiner stringJoiner = new StringJoiner(" and ", "Missing ", " coordinate in entity type dto.");
            if (entityTypeDTO.getXCoordinate() == null) {
                stringJoiner.add("X");
            }
            if (entityTypeDTO.getYCoordinate() == null) {
                stringJoiner.add("Y");
            }
            //noinspection HardcodedFileSeparator
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    stringJoiner.toString()
            );
        }
        final DataSourceSchema loadedSchema = peekDataSourceSchema(findDataSourceOrThrow(dataSourceUuid), true);
        for (de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType entityType : loadedSchema.getSemanticModel().getEntityTypes()) {
            if (entityType.getId().equals(entityTypeResourceId)) {
                Double xCoordinate = entityTypeDTO.getXCoordinate();
                Double yCoordinate = entityTypeDTO.getYCoordinate();
                if (xCoordinate != null && yCoordinate != null) {
                    entityType.setPosition(new Position(xCoordinate, yCoordinate));
                    entityTypeRepository.save(entityType);
                }
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Datasource {" + dataSourceUuid + "} does not contain specified entity type {" + entityTypeDTO + "}."
        );
    }

    @NotNull
    @Transactional
    public DataSourceSchema removeEntityTypeFromSchema(@NotNull UUID dataSourceUuid, @NotNull Long entityTypeResourceId) {
        //load schema copy
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        final DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        if (!newSchema.remove(new Traversable.Identity<>(entityTypeResourceId))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove entity type. Removing would cause inconsistencies.");
        }
        dataSourceModel.pushDataSourceSchema(newSchema);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @NotNull
    @Transactional
    public DataSourceSchema addRelation(@NotNull UUID dataSourceUuid, @NotNull RelationDTO relationDTO) {
        //deserialize
        Relation receivedRelation = semanticModelDTOConverter.fromDTO(relationDTO);

        if (receivedRelation.getId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New relation must not have an id."
            );
        }

        //load
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        //compute relation concept
        final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept usedRelationConcept;
        if (receivedRelation.getRelationConcept().getId() != null) {
            usedRelationConcept = loadFromRepository(receivedRelation.getRelationConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept::getUuid, relationConceptRepository);
        } else {
            usedRelationConcept = getAndUpdateCache(receivedRelation.getRelationConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept::getUuid, newSchema.getRelationConceptCache());
        }

        //update referenced entity types to matching ones from schema copy
        Traversable from = newSchema.find(receivedRelation.getFrom().getIdentity());
        if (from == null) {
            logger.warn(
                    "Failed to find tail entity type with id {} and identity {} in schema copy.",
                    receivedRelation.getFrom().getId(),
                    receivedRelation.getFrom().getIdentity()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id of tail entity of relation is not valid: " + receivedRelation.getFrom().getId());
        }

        Traversable to = newSchema.find(receivedRelation.getTo().getIdentity());
        if (to == null) {
            logger.warn(
                    "Failed to find head entity type with id {} and identity {} in schema copy.",
                    receivedRelation.getTo().getId(),
                    receivedRelation.getTo().getIdentity()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id of head entity of relation is not valid: " + receivedRelation.getTo().getId());
        }

        final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType fromEntityType = ObjectUtilities.checkedReturn(from, de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType.class);
        final de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType toEntityType = ObjectUtilities.checkedReturn(to, de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType.class);

        //update model
        receivedRelation.setRelationConcept(usedRelationConcept);
        receivedRelation.setFrom(fromEntityType);
        receivedRelation.setTo(toEntityType);
        newSchema.getSemanticModel().getRelations().add(receivedRelation);

        //persist and return copy
        dataSourceModel.pushDataSourceSchema(newSchema);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @NotNull
    @Transactional
    public DataSourceSchema updateRelation(@NotNull UUID dataSourceUuid, @NotNull Long relationResourceId, @NotNull RelationDTO relationDTO) {
        final Relation retrievedRelation = semanticModelDTOConverter.fromDTO(relationDTO);
        if (!relationResourceId.equals(relationDTO.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed id in relation is inconsistent to resource to update. Resource: " + relationResourceId + " Payload: " + relationDTO.getId()
            );
        }

        if (retrievedRelation.getRelationConcept().getId() == null && retrievedRelation.getRelationConcept().getUuid() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Creating new relation concepts is not supported on updating a relation. Either set id or uuid of referenced relation concept."
            );
        }

        //load schema copy
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        final DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept relationConcept = loadFromRepository(retrievedRelation.getRelationConcept(), de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.RelationConcept::getUuid, relationConceptRepository);
        retrievedRelation.setRelationConcept(relationConcept);

        Relation newRelation = retrievedRelation.copy();

        newSchema.replace(retrievedRelation.getIdentity(), newRelation);

        dataSourceModel.pushDataSourceSchema(newSchema);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @NotNull
    @Transactional
    public DataSourceSchema removeRelationFromSchema(@NotNull UUID dataSourceUuid, @NotNull Long relationResourceId) {
        //load schema copy
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        final DataSourceSchema newSchema = peekDataSourceSchema(dataSourceModel, true).copy();

        if (!newSchema.remove(new Traversable.Identity<>(relationResourceId))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove entity type. Removing would cause inconsistencies.");
        }

        dataSourceModel.pushDataSourceSchema(newSchema);
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    /* Caching operations */

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
        CombinedModel combinedModel = peekDataSourceSchema(dataSourceModel, true);

        //compute entity concept
        final EntityConcept usedEntityConcept = getAndUpdateCache(entityConcept, EntityConcept::getUuid, combinedModel.getEntityConceptCache());

        dataSourceRepository.save(dataSourceModel);

        if (usedEntityConcept.getId() != null) {
            return usedEntityConcept;
        } else {
            for (EntityConcept entityConcept : combinedModel.getEntityConceptCache()) {
                if (entityConcept.getName().equals(usedEntityConcept.getName()) && entityConcept.getDescription().equals(usedEntityConcept.getDescription())) {
                    return entityConcept;
                }
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cache entity concept.");
        }
    }

    @Transactional
    public void uncacheEntityConcept(@NotNull UUID dataSourceUuid, String entityConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = peekDataSourceSchema(dataSourceModel, true);

        boolean change = combinedModel.getSemanticModel().getProvisionalEntityConcepts().removeIf(o -> o.getUuid().equals(entityConceptUuid));
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
        CombinedModel combinedModel = peekDataSourceSchema(dataSourceModel, true);

        //compute entity concept
        final RelationConcept usedRelationConcept = getAndUpdateCache(
                receivedRelationConcept,
                RelationConcept::getUuid,
                combinedModel.getRelationConceptCache()
        );

        dataSourceRepository.save(dataSourceModel);

        if (usedRelationConcept.getId() != null) {
            return usedRelationConcept;
        } else {
            for (RelationConcept relationConcept : combinedModel.getRelationConceptCache()) {
                if (relationConcept.getName().equals(usedRelationConcept.getName()) && relationConcept.getDescription().equals(usedRelationConcept
                        .getDescription())) {
                    return relationConcept;
                }
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cache entity concept.");
        }
    }

    @Transactional
    public void uncacheRelationConcept(@NotNull UUID dataSourceUuid, String relationConceptUuid) {
        DataSourceModel dataSourceModel = findDataSourceOrThrow(dataSourceUuid);
        CombinedModel combinedModel = peekDataSourceSchema(dataSourceModel, true);

        boolean change = combinedModel.getSemanticModel().getProvisionalRelationConcepts().removeIf(o -> o.getUuid().equals(relationConceptUuid));
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
    private <T extends ModelBase> T getAndUpdateCache(
            @NotNull T element,
            @NotNull Function<T, String> identityFunction,
            @NotNull List<T> elementCache
    ) {
        final T result;
        if (element.getId() != null) {
            throw new IllegalArgumentException("Element to cache must not have a numeric id.");
        }
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
// TODO check how to realize (versioning, storage...)
    @NotNull
    @Transactional
    public DataSourceSchema undoDataSourceSchemaModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));
        dataSourceModel.popDataSourceSchema();
        dataSourceModel = dataSourceRepository.save(dataSourceModel);
        return dataSourceModel.peekDataSourceSchema();
    }

    @NotNull
    @Transactional
    public DataSourceSchema redoDataSourceSchemaModification(@NotNull UUID dataSourceUuid) {
        //Get the data source from the repository
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));

        DataSourceSchema schema = dataSourceModel.restoreDataSourceSchema();
        dataSourceRepository.save(dataSourceModel);
        return schema;
    }

    /* Finish operations */

    @NotNull
    public DataSourceSchema finishModeling(@NotNull UUID dataSourceUuid) {
        //Load the data source from the repository.
        DataSourceModel dataSourceModel = dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data Source does not exists"
                ));

        final CombinedModel combinedModel = dataSourceModel.peekDataSourceSchema();

        combinedModel.validate();

        final Pair<SemanticModel, Map<Long, String>> semanticModelCreationResult = createSemanticModel(combinedModel, dataSourceUuid.toString());
        SemanticModel semanticModel = semanticModelCreationResult.getLeft();
        final Map<Long, String> middlewareToActualUUIDMapping = semanticModelCreationResult.getRight();

        DataSourceSchema finalDataSourceSchema = createFinalDataSourceSchema(combinedModel, semanticModel, middlewareToActualUUIDMapping);
        dataSourceModel.pushDataSourceSchema(finalDataSourceSchema);
        DataSourceModel finalDataSource = dataSourceRepository.save(dataSourceModel);
        return finalDataSource.peekDataSourceSchema();
    }

    @NotNull
    private Pair<SemanticModel, Map<Long, String>> createSemanticModel(@NotNull CombinedModel combinedModel, @NotNull String dataSourceUuid) {
        // merge the concepts to not create possible new concepts twice
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

        //parse response and resolve id mapping
        final Map<Long, String> middlewareToActualKGSIdLookup = MapUtilities.compose(
                middlewareToRequestIdLookup,
                MapUtilities.mapValues(semanticModelCreationResponse.getOldToNewIdLookup(), Function.identity())
        );
        final SemanticModel convertedModel = knowledgeGraphServiceDTO2ModelConverter.convert(semanticModelCreationResponse.getSemanticModelDTO());

        return Pair.of(convertedModel, middlewareToActualKGSIdLookup);
    }

    @NotNull
    private DataSourceSchema createFinalDataSourceSchema(
            @NotNull DataSourceSchema currentDataSourceSchema,
            @NotNull SemanticModel retrievedSemanticModel,
            @NotNull Map<Long, String> modeling2KGSmapping
    ) {
        Map<Traversable.Identity<?>, Traversable> oldIdentityToNewTraversable = new HashMap<>();
        final Node newSyntaxModel = currentDataSourceSchema.getSyntaxModel().copy(oldIdentityToNewTraversable);

        //find all entity types which are mapped by a primitive node
        final Set<EntityType> mappedEntityTypes = oldIdentityToNewTraversable.values().stream()
                .filter(PrimitiveNode.class::isInstance)
                .map(PrimitiveNode.class::cast)
                .filter(p -> p.getEntityType() != null)
                .map(PrimitiveNode::getEntityType)
                .collect(Collectors.toSet());

        for (EntityType mappedEntityType : mappedEntityTypes) {
            final Traversable.Identity<?> oldEntityTypeIdentity = MapUtilities.keyOfValue(oldIdentityToNewTraversable, mappedEntityType);
            final Traversable traversable = currentDataSourceSchema.find(oldEntityTypeIdentity);
            assert traversable instanceof EntityType;
            EntityType oldEntityType = (EntityType) traversable;
            final String actualEntityTypeUuid = modeling2KGSmapping.get(oldEntityType.getId());

            EntityType replacement = null;
            for (EntityType entityType : retrievedSemanticModel.getEntityTypes()) {
                if (actualEntityTypeUuid.equals(entityType.getUuid())) {
                    replacement = entityType;
                    break;
                }
            }
            if (replacement == null) {
                logger.warn(
                        "Defective response for semantic model creation.\n" + "Mapping: {}\n" + "SemanticModel: {}",
                        modeling2KGSmapping,
                        retrievedSemanticModel
                );
                throw new RuntimeException(
                        "Failed to find entity type for id `" + oldEntityType.getId() + "` which should be mapped to `" + actualEntityTypeUuid + "`."
                );
            }
            newSyntaxModel.replace(mappedEntityType.getIdentity(), replacement);
        }

        //Update positions
        //Create a map between all old entity types and their new backend ID
        Map<String, EntityType> backendIDtoOldEntityTypeMapping =
                currentDataSourceSchema.getSemanticModel().getEntityTypes().stream().collect(Collectors.toMap(
                        x -> modeling2KGSmapping.get(x.getId()),
                        Function.identity()
                ));

        //Update positions
        for (EntityType entityType : retrievedSemanticModel.getEntityTypes()) {
            entityType.setPosition(backendIDtoOldEntityTypeMapping.get(entityType.getUuid()).getPosition());
        }

        DataSourceSchema finalSchema = new DataSourceSchema(
                currentDataSourceSchema.getOwningNode(),
                newSyntaxModel,
                retrievedSemanticModel,
                Collections.emptyList(),
                Collections.emptyList()
        );
        finalSchema.setFinalized(true);
        return finalSchema;
    }

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

    /**
     * Loads a ModelBase element from the given repository and validates if the loaded element's UUID matches the given element's UUID in relation to the
     * passed globalIdFunction.
     *
     * @param element       the element to load
     * @param uuidGenerator the function to determine a global UUID from the given elements type
     * @param repository    the repository to load from
     * @return attached element matching the given transient element
     */
    @NotNull
    private <T extends ModelBase> T loadFromRepository(
            @NotNull T element,
            @NotNull Function<T, String> uuidGenerator,
            @NotNull JpaRepository<T, Long> repository
    ) {
        String uuid = uuidGenerator.apply(element);
        Long longId = element.getId();
        final T result = repository.findById(longId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Not a valid " + element.getClass().getSimpleName() + " id: " + longId
                ));

        if (uuid != null && !uuid.equals(uuidGenerator.apply(result))) {
            //noinspection MagicCharacter
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inconsistent " + element.getClass().getSimpleName() + " received: id and uuid did not match data base state."
            );
        }
        return result;
    }

    @NotNull
    private DataSourceSchema peekDataSourceSchema(@NotNull DataSourceModel dataSourceModel, boolean schemaWillBeModified) {
        DataSourceSchema currentSchema = dataSourceModel.peekDataSourceSchema();
        if (schemaWillBeModified && currentSchema.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalized models are not allowed to be modified");
        }
        return currentSchema;
    }


    @NotNull
    private DataSourceModel findDataSourceOrThrow(@NotNull UUID dataSourceUuid) {
        return dataSourceRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data source uuid " + dataSourceUuid + ". "));
    }
}