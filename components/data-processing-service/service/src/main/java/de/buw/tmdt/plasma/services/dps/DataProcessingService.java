package de.buw.tmdt.plasma.services.dps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "de.buw.tmdt.plasma")
public class DataProcessingService {
	private static final Logger logger = LoggerFactory.getLogger(DataProcessingService.class);

	public static void main(String[] args) {
		final ConfigurableApplicationContext context = SpringApplication.run(DataProcessingService.class, args);
		final String applicationName = context.getEnvironment().getProperty("spring.application.name");
		logger.info("{}: Booting successful", applicationName);
	}
}
