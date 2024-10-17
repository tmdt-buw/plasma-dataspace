package de.buw.tmdt.plasma.services.sms.shared.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


@RequestMapping("/api/plasma-sms/ars-registration")
public interface ARSRegistrationAPI {
    // might be a PUT if the services update URL on re-registration
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    void ARSRegistration(RecommendationServiceRegistration registration);

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<RecommendationServiceRegistration> ARSList();
}
