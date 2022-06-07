package de.buw.tmdt.plasma.services.dps.shared.feignclient;

import de.buw.tmdt.plasma.services.sas.shared.api.AnalysisApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
		name = "plasma-dps",
		contextId = "data-processing-client",
		configuration = DataProcessingFeignConfiguration.class
)
public interface DataProcessingApiClient extends AnalysisApi {
}
