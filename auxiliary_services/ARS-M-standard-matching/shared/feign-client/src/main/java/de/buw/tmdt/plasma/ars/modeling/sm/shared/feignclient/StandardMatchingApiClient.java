package de.buw.tmdt.plasma.ars.modeling.sm.shared.feignclient;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.api.StandardMatchingAPI;
import org.springframework.cloud.openfeign.FeignClient;

@SuppressWarnings("HardcodedFileSeparator - uris")
@FeignClient(
        name = "plasma-ars-m-sm",
        contextId = "plasma-ars",
        configuration = StandardMatchingFeignConfiguration.class
)
public interface StandardMatchingApiClient extends StandardMatchingAPI {

}