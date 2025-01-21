package de.buw.tmdt.plasma.services.sms.shared.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("/api/sms/ars/registration")
public interface ARSServiceAPI {
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    void arsRegistration(RecommendationServiceRegistration registration);

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<RecommendationServiceRegistration> arsList();
}
