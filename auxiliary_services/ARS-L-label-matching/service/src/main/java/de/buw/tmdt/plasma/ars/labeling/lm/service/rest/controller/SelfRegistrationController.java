package de.buw.tmdt.plasma.ars.labeling.lm.service.rest.controller;

import de.buw.tmdt.plasma.ars.labeling.lm.service.handler.SelfRegistrationHandler;
import de.buw.tmdt.plasma.ars.labeling.lm.shared.api.SelfRegistrationAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SelfRegistrationController implements SelfRegistrationAPI {

    // autowiring can be removed if registration doesn't get more features
    private final SelfRegistrationHandler selfRegistrationHandler;

    @Autowired
    public SelfRegistrationController(SelfRegistrationHandler selfRegistrationHandler) {
        this.selfRegistrationHandler = selfRegistrationHandler;
    }

    @Override
    public void heartbeat() {}
}
