package de.buw.tmdt.plasma.services.dss.shared.feignclient;

import de.buw.tmdt.plasma.services.dss.shared.api.DataSourceApi;
import org.springframework.cloud.openfeign.FeignClient;

@SuppressWarnings("HardcodedFileSeparator - uris")
@FeignClient(
        name = "plasma-dss",
        contextId = "plasma-dss",
        configuration = DataSourceFeignConfiguration.class
)
public interface DataSourceApiClient extends DataSourceApi {

}