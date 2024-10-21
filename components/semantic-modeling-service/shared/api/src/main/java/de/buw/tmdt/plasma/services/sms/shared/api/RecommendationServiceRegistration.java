package de.buw.tmdt.plasma.services.sms.shared.api;

import java.time.LocalDateTime;

public class RecommendationServiceRegistration {
    private String serviceName;
    private String url;
    private LocalDateTime lastActive;
    private String uuid;
    private RecommendationType recommendationType;
    private String description;

    public enum RecommendationType {
        Labeling,
        Refinement,
        Modeling
    }

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecommendationType getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(RecommendationType recommendationType) {
        this.recommendationType = recommendationType;
    }

    public RecommendationServiceRegistration() {

    }
}
