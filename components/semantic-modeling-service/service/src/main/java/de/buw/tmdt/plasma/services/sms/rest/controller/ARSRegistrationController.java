package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.services.sms.ars.ARSService;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import de.buw.tmdt.plasma.services.sms.shared.api.ARSServiceAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("ARS Registration Controller")
public class ARSRegistrationController implements ARSServiceAPI {
    private final ARSService arsRegistrationHandler;

    @Autowired
    public ARSRegistrationController(ARSService arsRegistrationHandler) {
        this.arsRegistrationHandler = arsRegistrationHandler;
    }

    @Override
    public void ARSRegistration(@RequestBody RecommendationServiceRegistration registration) {
        arsRegistrationHandler.register(registration);
    }

    @Override
    public List<RecommendationServiceRegistration> ARSList() {
        return arsRegistrationHandler.getServicesAlive();
    }
}
