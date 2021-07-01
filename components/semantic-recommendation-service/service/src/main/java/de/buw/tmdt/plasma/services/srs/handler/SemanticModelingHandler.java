package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.feignclient.StandardMatchingApiClient;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class SemanticModelingHandler {

    private static final Logger log = LoggerFactory.getLogger(SemanticModelingHandler.class);

    final StandardMatchingApiClient standardMatchingApiClient;

    public SemanticModelingHandler(StandardMatchingApiClient standardMatchingApiClient) {
        this.standardMatchingApiClient = standardMatchingApiClient;
    }

    /**
     * Calls one or more ARS-M services.
     *
     * @param uuid          The data source uuid which services use to associate results
     * @param combinedModel The current state of the model
     * @return The updated model or the original one if no services were called
     */
    @NotNull
    public CombinedModel performModeling(@NotNull String uuid, String configId, @NotNull CombinedModel combinedModel) {
        // Call ARS - Modeling - Standard Matching
        try {
            combinedModel = standardMatchingApiClient.fetchResult(uuid);
        } catch (Exception e) {
            log.warn("Error calling ARS-M Standard Matching: ", e);
        }
        return combinedModel;
    }


}
