package de.buw.tmdt.plasma.services.sms.core;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ARSRegistrationHandler {
    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());
    public ARSRegistrationHandler() {
    }
    public void register(RecommendationServiceRegistration registration) {
        // add validation or something
        registration.setMessage("Registered");
        servicesAlive.add(registration);
    }
    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }
}
