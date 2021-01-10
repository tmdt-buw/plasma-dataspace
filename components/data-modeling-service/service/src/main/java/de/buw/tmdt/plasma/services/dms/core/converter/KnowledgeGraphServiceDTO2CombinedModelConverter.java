package de.buw.tmdt.plasma.services.dms.core.converter;

import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.RelationConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.EntityTypeDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.RelationDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Converter between the KGS data model and the shared CombinedModel.
 */
@Service
public class KnowledgeGraphServiceDTO2CombinedModelConverter {

    /**
     * Converts a whole KGS {@link SemanticModelDTO} to a {@link SemanticModel} of a {@link de.buw.tmdt.plasma.datamodel.CombinedModel}.
     *
     * @param semanticModelDTO The KGS DTO to convert
     * @return The converted {@link SemanticModel}.
     * @throws IllegalStateException If a relation points to a non-existing {@link EntityTypeDTO} id.
     */
    @NotNull
    public SemanticModel convert(@NotNull SemanticModelDTO semanticModelDTO) throws IllegalStateException {
        /*
         * Maps KGS ET database ids, identified by {@link EntityTypeDTO#id} to {@link EntityType#uuid}s in converted elements.
         * Needed to instantiate edges.
         */
        HashMap<String, String> entityTypeIdMap = new HashMap<>();
        // convert all entity types (with entity concepts enclosed)
        List<EntityType> entityTypes = new ArrayList<>();
        for (EntityTypeDTO entityTypeDTO : semanticModelDTO.getNodes()) {
            EntityType convert = convert(entityTypeDTO);
            entityTypes.add(convert);
            entityTypeIdMap.put(entityTypeDTO.getId(), convert.getUuid());
        }
        // convert all relations (with relation concepts enclosed)
        List<Relation> relations = new ArrayList<>();
        for (RelationDTO relationDTO : semanticModelDTO.getEdges()) {
            Relation convert = convert(relationDTO, entityTypeIdMap);
            relations.add(convert);
        }

        return new SemanticModel(semanticModelDTO.getId(), entityTypes, relations);
    }


    /**
     * Checks if a specific {@link EntityConcept} already exists in the semantic model.
     * If not, it is created using {@link #convert(EntityConceptDTO)}.
     *
     * @param semanticModel       The semantic model to use as a reference
     * @param kgsEntityConceptDTO The DTO to check
     * @return The existing or converted {@link EntityConcept}
     */
    private EntityConcept convertIfAbsent(SemanticModel semanticModel, EntityConceptDTO kgsEntityConceptDTO) {
        return semanticModel.getNodes().stream()
                .map(EntityType::getEntityConcept)
                .filter(ec -> ec.getSourceURI().equals(kgsEntityConceptDTO.getSourceURI()))
                .findFirst().orElseGet(() -> convert(kgsEntityConceptDTO));
    }

    protected EntityType convert(@NotNull EntityTypeDTO kgsEntityTypeDTO) {
        EntityConcept entityConcept = convert(kgsEntityTypeDTO.getConcept());

        EntityType et = new EntityType(
                kgsEntityTypeDTO.getLabel(),
                kgsEntityTypeDTO.getId(),
                kgsEntityTypeDTO.getXCoordinate(),
                kgsEntityTypeDTO.getYCoordinate(),
                kgsEntityTypeDTO.getDescription() != null ? kgsEntityTypeDTO.getDescription() : "",
                entityConcept
        );
        et.setOriginalLabel(kgsEntityTypeDTO.getOriginalLabel());
        return et;
    }

    @NotNull
    public EntityConcept convert(
            @NotNull EntityConceptDTO entityConceptDTO
    ) {
        return new EntityConcept(
                UUID.randomUUID().toString(),
                entityConceptDTO.getMainLabel(),
                entityConceptDTO.getId(),
                entityConceptDTO.getDescription(),
                entityConceptDTO.getSourceURI()
        );
    }

    @NotNull
    protected Relation convert(
            @NotNull RelationDTO kgsDto,
            HashMap<String, String> entityTypeIdMap) throws IllegalStateException {
        String fromUuid = entityTypeIdMap.get(kgsDto.getFromId());
        if (fromUuid == null) {
            throw new IllegalStateException("Undefined reference in 'from' field: " + kgsDto.getFromId());
        }
        String toUuid = entityTypeIdMap.get(kgsDto.getToId());
        if (toUuid == null) {
            throw new IllegalStateException("Undefined reference in 'to' field: " + kgsDto.getToId());
        }
        return new Relation(
                kgsDto.getId(),
                fromUuid,
                toUuid,
                convert(kgsDto.getConcept())
        );
    }

    @NotNull
    public RelationConcept convert(
            @NotNull RelationConceptDTO kgsDto
    ) {
        return new RelationConcept(
                kgsDto.getLabel(),
                kgsDto.getDescription(),
                kgsDto.getId(),
                kgsDto.getSourceURI(),
                kgsDto.getProperties()
        );
    }

    /**
     * Converts a {@link SemanticModel} to a {@link SemanticModelDTO}.
     * Existing entityIds are preserved and used as ids in the DTO.
     *
     * @param originalSemanticModel The {@link SemanticModel} to convert
     * @param dataSourceId          The data source uuid
     * @return The converted DTO
     * @throws CombinedModelIntegrityException If any integrity issues arose during the id conversion
     */
    @NotNull
    public SemanticModelDTO convert(@NotNull SemanticModel originalSemanticModel, @NotNull String dataSourceId) throws CombinedModelIntegrityException {
        // as we do some modifications here, create a separate instance
        SemanticModel semanticModel = originalSemanticModel.copy();

        // convert all objects to use the existing entityId if available, otherwise keep the UUID (will be dropped)
        // map to hold conversions from UUID -> EntityID if applied to any object (needed to adjust relations)
        HashMap<String, String> idMappings = new HashMap<>();

        // NOTE: we do  not change the actual uuids here, this will be done automatically during conversion of the elements
        // Entity Types
        semanticModel.getNodes().stream().filter(element -> element.getEntityId() != null)
                .forEach(element -> idMappings.put(element.getUuid(), element.getEntityId()));

        // now we have built the index, adjust the relations (as a separate step for clarity)
        semanticModel.getEdges().forEach(relation -> {
            // adjust fromId
            if (idMappings.containsKey(relation.getFromId())) {
                relation.setFromId(idMappings.get(relation.getFromId()));
            }
            // adjust toId
            if (idMappings.containsKey(relation.getToId())) {
                relation.setToId(idMappings.get(relation.getToId()));
            }
        });

        semanticModel.validate();

        // now that we have adjusted the ids, convert the elements
        final ArrayList<@NotNull EntityTypeDTO> entityTypes = CollectionUtilities.map(
                semanticModel.getNodes(),
                this::convert,
                ArrayList::new
        );
        final ArrayList<@NotNull RelationDTO> relations = CollectionUtilities.map(
                semanticModel.getEdges(),
                this::convert,
                ArrayList::new
        );

        return new SemanticModelDTO(
                semanticModel.getEntityId() != null ? semanticModel.getEntityId() : UUID.randomUUID().toString(),
                dataSourceId, // add meta information like versions here should this exist someday
                null,
                entityTypes,
                relations,
                dataSourceId
        );
    }

    @NotNull
    protected EntityTypeDTO convert(@NotNull EntityType entityType) {

        final EntityConceptDTO entityConceptDTO = convert(entityType.getEntityConcept());

        return new EntityTypeDTO(
                entityType.getEntityId() == null ? entityType.getUuid() : entityType.getEntityId(),
                entityType.getLabel(),
                entityType.getOriginalLabel(),
                entityConceptDTO,
                entityType.getXCoordinate() == null ? Double.valueOf(0) : entityType.getXCoordinate(),
                entityType.getYCoordinate() == null ? Double.valueOf(0) : entityType.getYCoordinate(),
                entityType.getDescription(),
                entityType.getSyntaxNodeUuid() != null
        );
    }

    @NotNull
    protected EntityConceptDTO convert(@NotNull EntityConcept entityConcept) {

        return new EntityConceptDTO(
                entityConcept.getEntityId() == null ? entityConcept.getUuid() : entityConcept.getEntityId(),
                entityConcept.getLabel(),
                entityConcept.getDescription() == null ? "" : entityConcept.getDescription(),
                entityConcept.getSourceURI()
        );
    }

    @NotNull
    protected RelationDTO convert(@NotNull Relation relation) {
        final RelationConceptDTO relationConceptDTO = convert(relation.getConcept());

        return new RelationDTO(
                relation.getEntityId() == null ? relation.getUuid() : relation.getEntityId(),
                relation.getFromId(),
                relation.getToId(),
                relationConceptDTO
        );
    }

    @NotNull
    protected RelationConceptDTO convert(@NotNull RelationConcept relationConcept) {

        return new RelationConceptDTO(
                relationConcept.getEntityId() == null ? relationConcept.getUuid() : relationConcept.getEntityId(),
                relationConcept.getLabel(),
                relationConcept.getDescription(),
                relationConcept.getSourceURI(),
                new HashSet<>() // TODO adjust once properly filled
        );
    }
}
