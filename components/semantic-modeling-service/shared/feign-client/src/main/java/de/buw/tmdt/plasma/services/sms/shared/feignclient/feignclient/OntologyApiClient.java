package de.buw.tmdt.plasma.services.sms.shared.feignclient.feignclient;

import de.buw.tmdt.plasma.services.sms.shared.api.OntologyApi;
import de.buw.tmdt.plasma.services.sms.shared.feignclient.KnowledgeGraphFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "plasma-kgs",
        contextId = "universal-graph-kgs-client",
        configuration = KnowledgeGraphFeignConfiguration.class
)
public interface OntologyApiClient extends OntologyApi {

}