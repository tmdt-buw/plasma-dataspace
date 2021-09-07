package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.ars.refinement.recommendation.shared.feignclient.feignclient.RecommendationApiClient;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SemanticRefinementHandler {

    private static final Logger log = LoggerFactory.getLogger(SemanticModelingHandler.class);

    final RecommendationApiClient recommendationApiClient;

    public SemanticRefinementHandler(RecommendationApiClient recommendationApiClient) {
        this.recommendationApiClient = recommendationApiClient;
    }


    @NotNull
    public CombinedModel performRefinement(@NotNull String uuid, String configId, String configToken, @NotNull CombinedModel combinedModel) {

        // call ARS-R here
        try {
            combinedModel = recommendationApiClient.recommend(combinedModel);
        } catch (Exception e) {
            log.warn("Error calling ARS-R Recommendation: ", e);
        }
        // TODO ensure that all recommendations have a non-empty set of anchor points
        return combinedModel;
    }

}
