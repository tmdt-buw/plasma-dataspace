package de.buw.tmdt.plasma.services.dms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "de.buw.tmdt.plasma")
public class DataModelingService {
	private static final Logger logger = LoggerFactory.getLogger(DataModelingService.class);

	public static void main(String[] args) {
		final ConfigurableApplicationContext context = SpringApplication.run(DataModelingService.class, args);
		final String applicationName = context.getEnvironment().getProperty("spring.application.name");
		logger.info("{}: Booting successful", applicationName);
	}
}
