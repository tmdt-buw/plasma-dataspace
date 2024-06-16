package de.buw.tmdt.plasma.services.sms.shared.feignclient.feignclient;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients("de.buw.tmdt.plasma.services.dms.shared.feignclient")
@Configuration
public class DataModelingFeignConfiguration {

}