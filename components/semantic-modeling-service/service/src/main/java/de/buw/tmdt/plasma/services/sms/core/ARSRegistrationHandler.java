package de.buw.tmdt.plasma.services.sms.core;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;

@Service
public class ARSRegistrationHandler {
    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());

    public ARSRegistrationHandler() {}

    @Scheduled(fixedRateString = "${plasma.registration.ping.interval:30000}")
    public void pingServices() {
        if (servicesAlive.isEmpty()) {
            return;
        }
        synchronized (servicesAlive) {
            Iterator<RecommendationServiceRegistration> iteration = servicesAlive.iterator();
            while (iteration.hasNext()) {
                LocalDateTime now = LocalDateTime.now();
                RecommendationServiceRegistration service = iteration.next();
                Duration lastAlive = Duration.between(service.getLastActive(), now);
                if (lastAlive.getSeconds() > 30) {
                    iteration.remove();
                }
            }
        }
    }

    public void register(RecommendationServiceRegistration registration) {
        for (RecommendationServiceRegistration service : servicesAlive) {
            if (service.getUuid().equals(registration.getUuid())) return;
        }
        registration.setLastActive(LocalDateTime.now());
        servicesAlive.add(registration);
    }

    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }
}
