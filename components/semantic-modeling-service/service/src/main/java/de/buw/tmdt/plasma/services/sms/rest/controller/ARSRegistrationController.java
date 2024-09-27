package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.services.sms.core.ARSRegistrationHandler;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import de.buw.tmdt.plasma.services.sms.shared.api.ARSRegistrationAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("ARS Registration Controller")
public class ARSRegistrationController implements ARSRegistrationAPI {
    private final ARSRegistrationHandler arsRegistrationHandler;

    @Autowired
    public ARSRegistrationController(ARSRegistrationHandler arsRegistrationHandler) {
        this.arsRegistrationHandler = arsRegistrationHandler;
    }

    @Override
    public void ARSRegistration(@RequestBody RecommendationServiceRegistration registration) {
        arsRegistrationHandler.register(registration);
    }

    // debug. remove later
    @Override
    public List<RecommendationServiceRegistration> ARSList() {
        return arsRegistrationHandler.getServicesAlive();
    }
}
