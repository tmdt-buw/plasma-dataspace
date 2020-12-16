package de.buw.tmdt.plasma.services.srs.api.recommendation;

import de.buw.tmdt.plasma.services.srs.api.semanticmodel.EntityTypeDTO;
import de.buw.tmdt.plasma.services.srs.api.semanticmodel.RelationDTO;

import java.util.Set;

/**
 * Defines a Delta recommendation, a partial update of a semantic model
 */

public class DeltaRecommendationDTO {

    public Set<EntityTypeDTO> entities;

    public Set<RelationDTO> relations;

    public double confidence;

}
