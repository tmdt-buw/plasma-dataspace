package de.buw.tmdt.plasma.ars.modeling.sm.service.repository;

import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Transactional
@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, String> {

}
