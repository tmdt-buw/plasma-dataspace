package de.buw.tmdt.plasma.ars.modeling.sm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import sharedconfigs.ConfigService;
import sharedconfigs.VolumeConfigClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "de.buw.tmdt.plasma")
public class ARSMStandardMatchingService {
	private static final Logger logger = LoggerFactory.getLogger(ARSMStandardMatchingService.class);

	public static void main(String[] args) {
		final ConfigurableApplicationContext context = SpringApplication.run(ARSMStandardMatchingService.class, args);
		final String applicationName = context.getEnvironment().getProperty("spring.application.name");
		logger.info("{}: Booting successful", applicationName);
	}

	@Bean
	public ConfigService getConfigService() {
		return new VolumeConfigClient("C://temp");
	}
}
