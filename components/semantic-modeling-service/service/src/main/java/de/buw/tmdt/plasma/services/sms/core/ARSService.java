package de.buw.tmdt.plasma.services.sms.core;

import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service to handle Registration of ARS
 @author Sven Stopka
  * */
@Service
public class ARSService {
    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());

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

    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }

}
