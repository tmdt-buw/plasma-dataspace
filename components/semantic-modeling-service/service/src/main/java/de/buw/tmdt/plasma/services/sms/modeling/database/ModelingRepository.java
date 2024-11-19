package de.buw.tmdt.plasma.services.sms.modeling.database;

import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
public interface ModelingRepository extends JpaRepository<Modeling, String> {

}