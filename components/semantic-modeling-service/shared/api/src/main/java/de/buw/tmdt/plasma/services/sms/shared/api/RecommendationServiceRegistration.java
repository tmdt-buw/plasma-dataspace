package de.buw.tmdt.plasma.services.sms.shared.api;

import java.time.LocalDateTime;
import java.util.UUID;
import java.net.URL;

public class RecommendationServiceRegistration {
    private String serviceName;
    private URL url;
    private LocalDateTime lastActive;
    private UUID uuid;

    public URL getUrl() {
        return url;
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
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

    public void generateUuid() {
        this.uuid = UUID.randomUUID();
    }


    // DO NOT DELETE
    public RecommendationServiceRegistration() {
    }

    public RecommendationServiceRegistration(String serviceName, String url) {
        this.serviceName = serviceName;
        this.setUrl(url);
    }

}
