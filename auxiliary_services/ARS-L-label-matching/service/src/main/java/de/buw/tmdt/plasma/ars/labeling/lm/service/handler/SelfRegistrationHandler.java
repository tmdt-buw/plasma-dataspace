package de.buw.tmdt.plasma.ars.labeling.lm.service.handler;

import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.UUID;

@Service
public class SelfRegistrationHandler {
    @Value ("${plasma.sms.address}")
    private String upstreamHostName;
    @Value ("${plasma.server_address}")
    private String serverAddress;
    @Value ("${registration.upstream.port}")
    private Integer upstreamPort;

    private final UUID serviceId = UUID.randomUUID();
    private final RestTemplate restTemplate = new RestTemplate();

    public SelfRegistrationHandler() {
    }

    @Scheduled(fixedRateString = "${registration.interval:30000}")
    public void Main() {
        try {
            RecommendationServiceRegistration registrationPackage = new RecommendationServiceRegistration();

            registrationPackage.setServiceName("ars-l-label-matching");
            registrationPackage.setUrl(serverAddress);
            registrationPackage.setUuid(serviceId);

            URL upstreamUrl = new URL("http", upstreamHostName, upstreamPort,"/api/plasma-sms/ars-registration");

            ResponseEntity<String> response = restTemplate.postForEntity(upstreamUrl.toURI(), registrationPackage, String.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
