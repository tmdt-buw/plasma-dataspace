package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.feignclient.StandardMatchingApiClient;
import de.buw.tmdt.plasma.services.dms.shared.dto.DataSourceSchemaDTO;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.springframework.stereotype.Service;


@Service
public class SemanticModelingHandler {

    public CombinedModel performModeling(String uuid, CombinedModel combinedModel) {
        // call ARS-R here
        return combinedModel;
    final StandardMatchingApiClient standardMatchingApiClient;

    public SemanticModelingHandler(StandardMatchingApiClient standardMatchingApiClient) {
        this.standardMatchingApiClient = standardMatchingApiClient;
    }

}
