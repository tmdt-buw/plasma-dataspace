package de.buw.tmdt.plasma.ars.modeling.sm.service.converter;

import de.buw.tmdt.plasma.ars.modeling.sm.service.model.EntityRule;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.RelationRule;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.EntityRuleDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.RelationRuleDTO;
import org.springframework.stereotype.Service;

@Service
public class RuleDTOConverter {

    public EntityRule fromDTO(EntityRuleDTO entityRuleDTO) {
        return new EntityRule(
                "".equals(entityRuleDTO.getOriginalAttribute()) ? null : entityRuleDTO.getOriginalAttribute(),
                entityRuleDTO.getConceptName(),
                entityRuleDTO.getConceptDescription(),
                entityRuleDTO.getConceptSourceURI(),
                entityRuleDTO.getRequiredPredecessor()
        );
    }

    public RelationRule fromDTO(RelationRuleDTO relationRuleDTO) {
        return new RelationRule(
                relationRuleDTO.getFromConcept(),
                relationRuleDTO.getToConcept(),
                relationRuleDTO.getConceptSourceURI(),
                relationRuleDTO.getConceptName(),
                relationRuleDTO.getConceptDescription()
        );
    }
}
