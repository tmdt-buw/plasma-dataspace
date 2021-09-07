package de.buw.tmdt.plasma.ars.refinement.recommendation.shared.feignclient.feignclient;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients("de.buw.tmdt.plasma.ars.refinement.recommendation.shared.feignclient")
@Configuration
public class RecommendationFeignConfiguration {

}