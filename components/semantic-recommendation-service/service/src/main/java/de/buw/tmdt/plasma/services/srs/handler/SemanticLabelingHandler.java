package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.ars.labeling.lm.shared.feignclient.LabelMatchingApiClient;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SemanticLabelingHandler {

    private static final Logger log = LoggerFactory.getLogger(SemanticLabelingHandler.class);

    final LabelMatchingApiClient labelMatchingApiClient;

    public SemanticLabelingHandler(
            LabelMatchingApiClient labelMatchingApiClient) {
        this.labelMatchingApiClient = labelMatchingApiClient;
    }

    @NotNull
    public CombinedModel performLabeling(@NotNull String uuid, @NotNull CombinedModel combinedModel) {
        try {
            // call ARS-L here
            combinedModel = labelMatchingApiClient.performLabeling(uuid, combinedModel);
        } catch (Exception ex) {
            log.warn("Could not perform semantic labeling", ex);
        }

        return combinedModel;
    }
}
