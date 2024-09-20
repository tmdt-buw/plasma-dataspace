package de.buw.tmdt.plasma.services.sms.shared.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;



@RequestMapping("/api/plasma-sms/ars-registration")
public interface ARSRegistrationAPI {
    @PostMapping()
    RecommendationServiceRegistration ARSRegistration(RecommendationServiceRegistration registration);
}
