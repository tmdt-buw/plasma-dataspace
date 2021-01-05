package de.buw.tmdt.plasma.services.dms.core.repository;

import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DataSourceRepository extends MongoRepository<DataSourceModel, UUID> {

}