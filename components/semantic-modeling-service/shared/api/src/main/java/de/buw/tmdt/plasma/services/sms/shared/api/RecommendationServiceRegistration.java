package de.buw.tmdt.plasma.services.sms.shared.api;

import java.time.LocalDateTime;

public class RecommendationServiceRegistration {
    private String serviceName;
    private String url;
    private LocalDateTime lastActive;

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

    // DO NOT DELETE
    public RecommendationServiceRegistration() {
    }

    public RecommendationServiceRegistration(String serviceName, String url) {
        this.serviceName = serviceName;
        this.url = url;
    }

}
