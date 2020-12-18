package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.services.dms.shared.dto.DataSourceSchemaDTO;
import org.springframework.stereotype.Service;

@Service
public class SemanticRefinementHandler {
    public DataSourceSchemaDTO performRefinement(String uuid, DataSourceSchemaDTO dataSourceSchemaDTO) {
        // call ARS-R here
        return dataSourceSchemaDTO;
    }

}
