package de.buw.tmdt.plasma.services.sms.shared.api;

import java.time.LocalDateTime;
import java.util.UUID;

public class RecommendationServiceRegistration {
    private String serviceName;
    private String url;
    private LocalDateTime lastActive;
    private UUID uuid;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }


    // DO NOT DELETE
    public RecommendationServiceRegistration() {
    }

    public RecommendationServiceRegistration(String serviceName, String url) {
        this.serviceName = serviceName;
        this.setUrl(url);
    }

}
