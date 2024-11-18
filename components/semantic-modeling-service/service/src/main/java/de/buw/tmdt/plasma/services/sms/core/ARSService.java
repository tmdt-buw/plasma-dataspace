package de.buw.tmdt.plasma.services.sms.core;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.services.sms.core.model.Modeling;
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

/**
 * Service to handle Registration of ARS
 @author Sven Stopka
  * */
@Service
public class ARSService {

    @Value("${plasma.sms.feature.recommendations.enabled:false}")
    private Boolean recommendationsEnabled;

    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());
    private final Logger log = LoggerFactory.getLogger(ARSService.class);


    public ARSService() {}

    @Scheduled(fixedRateString = "${plasma.registration.ping.interval:30000}")
    public void checkLastAlive() {
        if (servicesAlive.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        servicesAlive.removeIf(service -> Duration.between(service.getLastActive(), now).getSeconds() > 30);
    }

    public void register(RecommendationServiceRegistration registration) {
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

    public CombinedModel performSemanticLabeling(Modeling modeling) throws ResponseStatusException {
        if (!recommendationsEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Recommendations are currently disabled.");
        }

        try {
            List<String> preferredLabeling = modeling.getPreferredLabelingService();
            preferredLabeling.forEach(service -> log.info("Labeling Service: {}", service));

            // nyi : get URL somehow?
        } catch (Exception e) {
            log.warn("Error performing labeling: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error performing labeling: ");
        }

        return modeling.getCurrentModel();
    }


    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }

}
