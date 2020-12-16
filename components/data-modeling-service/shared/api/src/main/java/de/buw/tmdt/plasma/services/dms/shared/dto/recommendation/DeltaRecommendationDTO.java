package de.buw.tmdt.plasma.services.dms.shared.dto.recommendation;

import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.EntityTypeDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.RelationDTO;

import java.util.Set;

/**
 * Defines a Delta recommendation, a partial update of a semantic model
 */
public class DeltaRecommendationDTO {

    /**
     * Recommended entities.
     * If {@link EntityTypeDTO#getUuid()} is set to an existing uuid in the model this is a replacement recommendation.
     * All {@link EntityTypeDTO#getId()} values are generated to be used in relations.
     */
    public Set<EntityTypeDTO> entities;

    /**
     * Recommended relations.
     */
    public Set<RelationDTO> relations;

    /**
     * A confidence score in [0,1]
     */
    public double confidence;

}
