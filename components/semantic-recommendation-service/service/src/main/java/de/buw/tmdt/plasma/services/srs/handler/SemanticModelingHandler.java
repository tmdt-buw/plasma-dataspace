package de.buw.tmdt.plasma.services.srs.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.feignclient.StandardMatchingApiClient;
import de.buw.tmdt.plasma.services.dms.shared.dto.DataSourceSchemaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class SemanticModelingHandler {

    final StandardMatchingApiClient standardMatchingApiClient;
    private final ObjectMapper objectMapper;

    public SemanticModelingHandler(StandardMatchingApiClient standardMatchingApiClient) {
        this.standardMatchingApiClient = standardMatchingApiClient;
        this.objectMapper = new ObjectMapper();
    }

    public DataSourceSchemaDTO performModeling(String uuid, DataSourceSchemaDTO dataSourceSchemaDTO) {
        // Call ARS - Modeling - Standard Matching
        SemanticModelDTO semanticModelDTOsm = standardMatchingApiClient.fetchResult(uuid);
        String sm;
        de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.SemanticModelDTO nm = null;
        try {
            sm = objectMapper.writeValueAsString(semanticModelDTOsm);
            nm = objectMapper.readValue(sm,
                    de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.SemanticModelDTO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return new DataSourceSchemaDTO(
                dataSourceSchemaDTO.getSyntaxModelDTO(),
                nm, new ArrayList<>(), new ArrayList<>(), false
        );
    }
}
