package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.rest.controller;

import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler.RecommendationHandler;
import de.buw.tmdt.plasma.ars.refinement.recommendation.shared.api.api.ARSRRecommendationAPI;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController("Recommendation Controller")
public class RecommendationController implements ARSRRecommendationAPI {

    private final RecommendationHandler recommendationHandler;

    @Autowired
    public RecommendationController(
            RecommendationHandler recommendationHandler) {
        this.recommendationHandler = recommendationHandler;
    }

    @Override
    @Operation(description = "generates recommendations for the given combined model")
    public @NotNull CombinedModel recommend(@NotNull CombinedModel combinedModel) {
        return recommendationHandler.generateRecommendations(combinedModel);
    }
}