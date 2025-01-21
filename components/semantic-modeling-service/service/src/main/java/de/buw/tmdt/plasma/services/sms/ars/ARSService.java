package de.buw.tmdt.plasma.services.sms.ars;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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


    public ARSService() {

    }

    @Scheduled(fixedRateString = "${plasma.registration.ping.interval:30000}")
    public void checkLastAlive() {
        if (servicesAlive.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        servicesAlive.removeIf(service -> Duration.between(service.getLastActive(), now).getSeconds() > 60);
    }

    public void register(RecommendationServiceRegistration registration) {
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

    public CombinedModel performLabeling(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
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

            Mono<CombinedModel> combinedModelMono = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("uuid", uuid)
                            .queryParam("configId", configId)
                            .queryParam("configToken", configToken)
                            .build()
                    )
                    .bodyValue(combinedModel)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(CombinedModel.class);
            CombinedModel withLabeling = combinedModelMono.block();
            log.info("Performed labeling for {} using service: {}", uuid, url);
            if (withLabeling == null) {
                log.warn("Result was NULL. Fallback to input model.");
                return combinedModel;
            }
            return withLabeling;
        } catch (Exception ex) {
            log.warn("Could not perform semantic labeling using service {}", url, ex);
        }

        return combinedModel;
    }

    @NotNull
    public CombinedModel performModeling(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
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

            Mono<CombinedModel> combinedModelMono = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("uuid", uuid)
                            .queryParam("configId", configId)
                            .queryParam("configToken", configToken)
                            .build()
                    )

                    .bodyValue(combinedModel)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(CombinedModel.class);
            CombinedModel withModeling = combinedModelMono.block();
            log.info("Performed modeling for {} using service: {}", uuid, url);
            if (withModeling == null) {
                log.warn("Result was NULL. Fallback to input model.");
                return combinedModel;
            }
            return withModeling;
        } catch (Exception ex) {
            log.warn("Could not perform semantic modeling using service {}", url, ex);
        }
        return combinedModel;
    }

    @NotNull
    public CombinedModel performRefinement(@NotNull String uuid, Modeling modeling, String configId, String configToken, @NotNull CombinedModel combinedModel) {
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

                Mono<CombinedModel> combinedModelMono = webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("uuid", uuid)
                                .queryParam("configId", configId)
                                .queryParam("configToken", configToken)
                                .build()
                        )

                        .bodyValue(combinedModel)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(CombinedModel.class);
                CombinedModel withRefinement = combinedModelMono.block();

                if (withRefinement == null) {
                    log.debug("Result was NULL.");
                    continue;
                }
                modifications.addAll(withRefinement.getRecommendations());
            } catch (Exception ex) {
                log.warn("Could not perform semantic refinement using service {}", service.getUrl(), ex);
            }
        }
        modifications.removeIf(dm -> !dm.getReference().startsWith(prefix));
        combinedModel.getRecommendations().addAll(modifications);

        return combinedModel;
    }

    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }

}
