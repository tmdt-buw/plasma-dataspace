package de.buw.tmdt.plasma.services.sms.shared.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


@RequestMapping("/api/plasma-sms/ars-registration")
public interface ARSRegistrationAPI {
    // might be a PUT if the services update URL on re-registration
    @PostMapping()
    void ARSRegistration(RecommendationServiceRegistration registration);

    // debug. remove later
    @GetMapping()
    List<RecommendationServiceRegistration> ARSList();
}
