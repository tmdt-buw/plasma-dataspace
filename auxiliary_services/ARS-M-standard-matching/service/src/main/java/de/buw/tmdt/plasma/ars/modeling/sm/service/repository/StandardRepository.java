package de.buw.tmdt.plasma.ars.modeling.sm.service.repository;

import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StandardRepository extends JpaRepository<Standard, UUID> {

    List<Standard> findAllByName(String name);

}
