package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.feignclient.StandardMatchingApiClient;
import de.buw.tmdt.plasma.services.dms.shared.dto.DataSourceSchemaDTO;
import org.springframework.stereotype.Service;


@Service
public class SemanticModelingHandler {

    final StandardMatchingApiClient standardMatchingApiClient;

    public SemanticModelingHandler(StandardMatchingApiClient standardMatchingApiClient) {
        this.standardMatchingApiClient = standardMatchingApiClient;
    }

    public DataSourceSchemaDTO performModeling(String uuid, DataSourceSchemaDTO dataSourceSchemaDTO) {
        // Call ARS - Modeling - Standard Matching
        return standardMatchingApiClient.fetchResult(uuid);
    }
}
