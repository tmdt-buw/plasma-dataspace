package de.buw.tmdt.plasma.services.dss.shared.feignclient;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients("de.buw.tmdt.plasma.services.dss.shared.feignclient")
@Configuration
public class DataSourceFeignConfiguration {

}