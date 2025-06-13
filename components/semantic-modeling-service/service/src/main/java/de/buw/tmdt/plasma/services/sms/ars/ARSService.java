package de.buw.tmdt.plasma.services.sms.ars;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ARSService {

    @Value("${plasma.sms.feature.recommendations.enabled:false}")
    private Boolean recommendationsEnabled;

    @Value("${plasma.ars.refinement.prefix:ARS-R-}")
    private String prefix;

    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());
    private final Logger log = LoggerFactory.getLogger(ARSService.class);

    private final int ARS_SERVICE_TIMEOUT_SECONDS = 120;

    public ARSService() {

    }

    @Scheduled(fixedRateString = "${plasma.registration.ping.interval:30000}")
    public void checkLastAlive() {
        if (servicesAlive.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        servicesAlive.removeIf(service -> Duration.between(service.getLastActive(), now).getSeconds() > ARS_SERVICE_TIMEOUT_SECONDS);
    }

    public void register(RecommendationServiceRegistration registration) {
        if (!recommendationsEnabled) {
            return;
        }
        log.debug("Received registration from {} ({})",registration.getServiceName(),registration.getUuid().substring(0,8));
        for (RecommendationServiceRegistration service : servicesAlive) {
            if (service.getUuid().equals(registration.getUuid())) {
                service.setLastActive(LocalDateTime.now());
                return;
            }
        }
        registration.setLastActive(LocalDateTime.now());
        servicesAlive.add(registration);
    }

    public List<DeltaModification> performLabeling(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
        List<RecommendationServiceRegistration> targetServices = servicesAlive.stream()
                .filter(service -> modeling.getPreferredLabelingService().contains(service.getUuid()))
                .collect(Collectors.toList());
        if (targetServices.isEmpty()) {
            log.debug("No labeling services selected");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No labeling services are selected to process your request.");
        }
        // we only get the first for now
        String url = targetServices.get(0).getUrl();
        try {
            // call ARS-L services here
            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Mono<List<DeltaModification>> recommendationsMono = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("uuid", uuid)
                            .queryParam("configId", configId)
                            .queryParam("configToken", configToken)
                            .build()
                    )

                    .bodyValue(combinedModel)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<>() {
                    });
            List<DeltaModification> recommendations = recommendationsMono.block();
            log.info("Performed labeling for {} using service: {}", uuid, url);
            if (recommendations == null) {
                log.warn("Result was NULL.");
                return new ArrayList<>();
            }
            return recommendations;
        } catch (Exception ex) {
            log.warn("Could not perform semantic labeling using service {}", url, ex);
        }
        return new ArrayList<>();
    }

    @NotNull
    public List<DeltaModification> performModeling(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
        List<RecommendationServiceRegistration> targetServices = servicesAlive.stream()
                .filter(service -> modeling.getPreferredModelingService().contains(service.getUuid()))
                .collect(Collectors.toList());
        if (targetServices.isEmpty()) {
            log.debug("No modeling services selected");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No labeling modeling are selected to process your request.");
        }
        // we only get the first for now
        String url = targetServices.get(0).getUrl();
        try {
            // call ARS-M services here
            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Mono<List<DeltaModification>> recommendationsMono = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("uuid", uuid)
                            .queryParam("configId", configId)
                            .queryParam("configToken", configToken)
                            .build()
                    )

                    .bodyValue(combinedModel)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<>() {
                    });
            List<DeltaModification> recommendations = recommendationsMono.block();
            log.info("Performed modeling for {} using service: {}", uuid, url);
            if (recommendations == null) {
                log.warn("Result was NULL.");
                return new ArrayList<>();
            }
            return recommendations;
        } catch (Exception ex) {
            log.warn("Could not perform semantic modeling using service {}", url, ex);
        }
        return new ArrayList<>();
    }

    @NotNull
    public List<DeltaModification> performRefinement(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
        List<RecommendationServiceRegistration> targetServices = servicesAlive.stream()
                .filter(service -> modeling.getPreferredRefiningService().contains(service.getUuid()))
                .collect(Collectors.toList());
        if (targetServices.isEmpty()) {
            log.debug("No refinement services selected");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No refinement services are selected to process your request.");
        }
        combinedModel.getRecommendations().removeIf(dm -> dm.getReference().startsWith(prefix));
        List<DeltaModification> modifications = new ArrayList<>();
        for (RecommendationServiceRegistration service : targetServices) {
            try {
                log.debug("Performing refinement for {} using service: {}", uuid, service.getUrl());
                WebClient webClient = WebClient.builder()
                        .baseUrl(service.getUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

                Mono<List<DeltaModification>> recommendationsMono = webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("uuid", uuid)
                                .queryParam("configId", configId)
                                .queryParam("configToken", configToken)
                                .build()
                        )

                        .bodyValue(combinedModel)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<>() {
                        });
                List<DeltaModification> recommendations = recommendationsMono.block();

                if (recommendations == null) {
                    log.debug("Result was NULL.");
                    continue;
                }
                modifications.addAll(recommendations);
            } catch (Exception ex) {
                log.warn("Could not perform semantic refinement using service {}", service.getUrl(), ex);
            }
        }
        modifications.removeIf(dm -> !dm.getReference().startsWith(prefix));

        return modifications;
    }

    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }

}
