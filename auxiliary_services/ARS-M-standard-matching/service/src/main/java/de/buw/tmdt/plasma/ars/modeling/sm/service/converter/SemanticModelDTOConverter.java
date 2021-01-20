package de.buw.tmdt.plasma.ars.modeling.sm.service.converter;

import de.buw.tmdt.plasma.ars.modeling.sm.service.model.semanticmodel.*;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.*;
import de.buw.tmdt.plasma.utilities.collections.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SemanticModelDTOConverter {

    public SemanticModelDTO toDTO(@NotNull SemanticModel semanticModel) {

        List<EntityTypeDTO> entityTypeDTOS = semanticModel.getEntityTypes().stream().map(this::toDTO).collect(java.util.stream.Collectors.toList());

        List<RelationDTO> relations = semanticModel.getRelations().stream()
                .map(this::toDTO)
                .collect(Collectors.getSerializableListCollector());

        return new SemanticModelDTO(
                String.valueOf(semanticModel.getId()),
                semanticModel.getUuid(),
                semanticModel.getLabel(),
                semanticModel.getDescription(),
                entityTypeDTOS,
                relations
        );
    }

    private EntityTypeDTO toDTO(@NotNull EntityType entityType) {
        Double xCoordinate = null;
        Double yCoordinate = null;
        if (entityType.getPosition() != null) {
            xCoordinate = entityType.getPosition().getXCoordinate();
            yCoordinate = entityType.getPosition().getYCoordinate();
        }

        EntityConceptDTO entityConceptDTO = toDTO(entityType.getEntityConcept());

        return new EntityTypeDTO(
                xCoordinate,
                yCoordinate,
                entityType.getId(),
                String.valueOf(entityType.getUuid() == null ? null : UUID.fromString(entityType.getUuid())),
                entityType.getLabel(),
                entityType.getOriginalLabel(),
                entityType.getDescription(),
                entityConceptDTO,
                entityType.getOriginalLabel() != null
        );
    }

    private EntityConceptDTO toDTO(EntityConcept entityConcept) {
        return new EntityConceptDTO(entityConcept.getId(), entityConcept.getUuid(), entityConcept.getName(), entityConcept.getDescription(), entityConcept.getSourceURI());
    }

    private RelationDTO toDTO(Relation relation) {
        return new RelationDTO(
                relation.getId(),
                relation.getFrom().getId(),
                relation.getTo().getId(),
                relation.getDescription(),
                toDTO(relation.getRelationConcept())
        );
    }

    private RelationConceptDTO toDTO(@NotNull RelationConcept relationConcept) {
        return new RelationConceptDTO(
                relationConcept.getId() != null ? relationConcept.getId().toString() : null,
                relationConcept.getUuid(),
                relationConcept.getName(),
                relationConcept.getDescription(),
                relationConcept.getSourceURI(),
                relationConcept.getProperties().stream().map(RelationConcept.Property::getName).collect(Collectors.getSerializableSetCollector())
        );
    }
}