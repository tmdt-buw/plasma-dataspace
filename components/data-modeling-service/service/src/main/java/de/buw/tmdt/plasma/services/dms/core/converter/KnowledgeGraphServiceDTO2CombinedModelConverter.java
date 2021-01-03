package de.buw.tmdt.plasma.services.dms.core.converter;

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
 * Converter between the KGS data model and the shared CombinedModel
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
    protected SemanticModel convert(@NotNull SemanticModelDTO semanticModelDTO) throws IllegalStateException {
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

    @NotNull
    public SemanticModelDTO convert(@NotNull SemanticModel semanticModel, @NotNull String dataSourceId) {
        String semanticModelId = semanticModel.getEntityId() != null ? semanticModel.getEntityId() : UUID.randomUUID().toString();

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
                semanticModelId,
                dataSourceId,
                null,
                entityTypes,
                relations,
                dataSourceId
        );
    }

    @NotNull
    protected EntityTypeDTO convert(@NotNull EntityType entityType) {

        final EntityConceptDTO entityConceptDTO = convert(entityType.getEntityConcept());

        final EntityTypeDTO entityTypeDTO = new EntityTypeDTO(
                entityType.getUuid(),
                entityType.getLabel(),
                entityType.getOriginalLabel(),
                entityConceptDTO,
                entityType.getXCoordinate() == null ? 0 : entityType.getXCoordinate(),
                entityType.getYCoordinate() == null ? 0 : entityType.getYCoordinate(),
                entityType.getDescription(),
                entityType.isMappedToData()
        );

        return entityTypeDTO;
    }


    @NotNull
    protected EntityConceptDTO convert(@NotNull EntityConcept entityConcept) {
        final EntityConceptDTO entityConceptDTO = new EntityConceptDTO(
                entityConcept.getUuid(),
                entityConcept.getLabel(),
                entityConcept.getDescription(),
                entityConcept.getSourceURI()
        );

        return entityConceptDTO;
    }

    @NotNull
    protected RelationDTO convert(@NotNull Relation relation) {

        final RelationConceptDTO relationConceptDTO = convert(relation.getConcept());

        final RelationDTO relationDTO = new RelationDTO(
                relation.getUuid(),
                relation.getFromId(),
                relation.getToId(),
                relationConceptDTO
        );

        return relationDTO;
    }

    @NotNull
    protected RelationConceptDTO convert(@NotNull RelationConcept relationConcept) {

        final RelationConceptDTO relationConceptDTO = new RelationConceptDTO(
                relationConcept.getUuid(),
                relationConcept.getLabel(),
                relationConcept.getDescription(),
                relationConcept.getSourceURI(),
                new HashSet<>() // TODO adjust once finished
        );

        return relationConceptDTO;
    }

}
