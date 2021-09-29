package de.buw.tmdt.plasma.ars.refinement.recommendation.shared.feignclient.feignclient;

import de.buw.tmdt.plasma.ars.refinement.recommendation.shared.api.api.ARSRRecommendationAPI;
import org.springframework.cloud.openfeign.FeignClient;

@SuppressWarnings("HardcodedFileSeparator - uris")
@FeignClient(
        name = "plasma-ars-r-sr",
        contextId = "plasma-ars-r",
        configuration = RecommendationFeignConfiguration.class
)
public interface RecommendationApiClient extends ARSRRecommendationAPI {

}