package de.buw.tmdt.plasma.services.kgs.shared.feignclient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients("de.buw.tmdt.plasma.services.kgs.shared.feignclient")
@Configuration
public class KnowledgeGraphFeignConfiguration {

    @Bean
    public CloseableHttpClient kgsClient() {
        return HttpClients.createDefault();
    }

}