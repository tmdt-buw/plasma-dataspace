package de.buw.tmdt.plasma.services.sms.shared.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("/api/plasma-sms/ars")
public interface ARSServiceAPI {
    @PostMapping(name = "/registration", consumes = MediaType.APPLICATION_JSON_VALUE)
    void ARSRegistration(RecommendationServiceRegistration registration);

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<RecommendationServiceRegistration> ARSList();
}
