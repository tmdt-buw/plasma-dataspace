package de.buw.tmdt.plasma.ars.modeling.sm.service.repository;

import de.buw.tmdt.plasma.ars.modeling.sm.service.model.semanticmodel.EntityConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.UUID;

@Transactional
@Repository
public interface EntityConceptRepository extends JpaRepository<EntityConcept, UUID> {

}
