package de.buw.tmdt.plasma.services.sms.core;
import de.buw.tmdt.plasma.services.sms.shared.api.RecommendationServiceRegistration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;

@Service
public class ARSRegistrationHandler {
    private final List<RecommendationServiceRegistration> servicesAlive = Collections.synchronizedList(new ArrayList<>());

    public ARSRegistrationHandler() {}

    @Scheduled(fixedRateString = "${plasma.registration.ping.interval}")
    public void pingServices() {
        if (servicesAlive.isEmpty()) {
            return;
        }
        synchronized (servicesAlive) {
            Iterator<RecommendationServiceRegistration> iteration = servicesAlive.iterator();
            while (iteration.hasNext()) {
                RecommendationServiceRegistration service = iteration.next();
                try {
                    URL serviceUrl = service.getUrl();
                    HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();

                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);

                    int responseCode = connection.getResponseCode();

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new Exception("Service response not okay: "+ responseCode);
                    }
                    service.setLastActive(LocalDateTime.now());
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                    // iteration.remove();
                }
            }
        }
    }

    public void register(RecommendationServiceRegistration registration) {
        for (RecommendationServiceRegistration service : servicesAlive) {
            if (service.getServiceName().equals(registration.getServiceName())) return;
        }
        registration.setLastActive(LocalDateTime.now());
        servicesAlive.add(registration);
    }

    public List<RecommendationServiceRegistration> getServicesAlive() {
        return servicesAlive;
    }
}
