package de.buw.tmdt.plasma.services.sms.shared.api;

public class RecommendationServiceRegistration {
    private String serviceName;
    private String message;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RecommendationServiceRegistration() {
    }

    public RecommendationServiceRegistration(String serviceName, String message) {
        this.serviceName = serviceName;
        this.message = message;
    }

}
