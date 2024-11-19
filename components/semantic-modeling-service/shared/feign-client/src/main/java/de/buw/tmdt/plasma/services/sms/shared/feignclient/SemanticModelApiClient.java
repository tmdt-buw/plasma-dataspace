package de.buw.tmdt.plasma.services.sms.shared.feignclient;

import de.buw.tmdt.plasma.services.kgs.shared.api.SemanticModelApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "plasma-kgs",
        contextId = "local-graph-kgs-client",
        configuration = KnowledgeGraphFeignConfiguration.class
)
public interface SemanticModelApiClient extends SemanticModelApi {

}