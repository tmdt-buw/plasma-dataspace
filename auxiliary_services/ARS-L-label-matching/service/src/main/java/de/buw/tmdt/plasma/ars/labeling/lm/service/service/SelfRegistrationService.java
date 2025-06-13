package de.buw.tmdt.plasma.ars.labeling.lm.service.service;

import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration.RecommendationType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.UUID;

/**
 * Service to send registration to SMS.
 @author Sven Stopka
* */
@Service
public class SelfRegistrationService {
    @Value("${plasma.host_name}")
    private String serviceName;
    @Value ("${plasma.server_address}")
    private String serverAddress;
    @Value ("${plasma.registration.upstream.host}")
    private String upstreamHostName;
    @Value ("${plasma.registration.upstream.port}")
    private Integer upstreamPort;
    @Value("${plasma.registration.path:/api/sms/ars/registration}")
    private String registrationPath;

    private final String serviceId = UUID.randomUUID().toString();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RecommendationType recommendationType = RecommendationType.Labeling;

    public SelfRegistrationService() {
    }

    @Scheduled(fixedRateString = "${plasma.registration.interval:30000}")
    public void registerSelf() {
        try {
            RecommendationServiceRegistration registrationPackage = new RecommendationServiceRegistration();

            registrationPackage.setServiceName(serviceName);
            registrationPackage.setUrl(serverAddress);
            registrationPackage.setUuid(serviceId);
            registrationPackage.setDescription("Semantic Label Recommendations");
            registrationPackage.setRecommendationType(recommendationType);

            URL upstreamUrl = new URL("http", upstreamHostName, upstreamPort, registrationPath);

            restTemplate.postForEntity(upstreamUrl.toURI(), registrationPackage, String.class);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
