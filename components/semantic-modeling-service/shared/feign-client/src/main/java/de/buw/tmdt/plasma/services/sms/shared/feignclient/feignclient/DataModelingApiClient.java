package de.buw.tmdt.plasma.services.sms.shared.feignclient.feignclient;

import de.buw.tmdt.plasma.services.sms.shared.api.ModelingAPI;
import org.springframework.cloud.openfeign.FeignClient;

@SuppressWarnings("HardcodedFileSeparator - uris")
@FeignClient(
        name = "plasma-dms",
        contextId = "plasma-services",
        configuration = DataModelingFeignConfiguration.class
)
public interface DataModelingApiClient extends ModelingAPI {

}