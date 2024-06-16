package de.buw.tmdt.plasma.services.sms.schemaanalysis.database;

import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.UUID;

@Transactional
@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

}
