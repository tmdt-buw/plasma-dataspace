package de.buw.tmdt.plasma.services.sms.core;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import org.springframework.stereotype.Service;

@Service
public class ARSRegistrationHandler {
    public ARSRegistrationHandler() {
    }
    public RecommendationServiceRegistration register(RecommendationServiceRegistration registration) {
        registration.setMessage("Registered");
        return registration;
    }
}
