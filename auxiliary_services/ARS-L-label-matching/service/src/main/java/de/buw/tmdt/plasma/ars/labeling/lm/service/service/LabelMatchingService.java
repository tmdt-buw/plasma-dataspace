package de.buw.tmdt.plasma.ars.labeling.lm.service.service;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.MappableSemanticNode;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
public class LabelMatchingService {

    private static final Logger log = LoggerFactory.getLogger(LabelMatchingService.class);

    @Value("${plasma.kgs.ontology.api}")
    private String kgsOntologyAPI;

    private WebClient ontologyWebClient;

    public LabelMatchingService() {
    }

    @PostConstruct
    public void init() {
        this.ontologyWebClient = WebClient.builder()
                .baseUrl(kgsOntologyAPI)
                .filter((request, next) -> {
                    log.info("Making request to: {}", request.url());
                    return next.exchange(request);
                })
                .build();
    }

    public CombinedModel performLabeling(String id, String configId, String configToken, CombinedModel combined) {
        List<String> mappedPrimitiveNodeIds = combined.getSemanticModel().getNodes().stream()
                .filter(SemanticNode::isMapped)
                .map(n -> (MappableSemanticNode) n)
                .map(MappableSemanticNode::getMappedSyntaxNodeUuid)
                .collect(Collectors.toList());
        SyntaxModel syntaxModel = combined.getSyntaxModel();
        for (SyntaxNode node : syntaxModel.getNodes()) {
            if (!(node instanceof PrimitiveNode) || mappedPrimitiveNodeIds.contains(node.getUuid())) {
                continue;
            }
            String label = node.getLabel();
            List<SemanticNode> candidates;
            candidates = ontologyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/nodes")
                            .queryParam("infix", label)
                            .queryParam("limit", 50)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<SemanticNode>>() {})
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (candidates == null) {
                candidates = new ArrayList<>();
            }

            List<Class> classes = candidates.stream()
                    .filter(c -> c instanceof Class)
                    .map(c -> (Class) c)
                    .collect(Collectors.toList());

            Optional<Class> matching = classes.stream().filter(clazz -> clazz.getLabel().equalsIgnoreCase(label)).findFirst();
            if (matching.isPresent()) {  // we only match on exact equality but case-insensitive
                // we found one or more, create a mapping
                Class clazz = matching.get();
                clazz.createInstance();
                clazz.setMappedSyntaxNodeLabel(node.getLabel());
                clazz.setMappedSyntaxNodeUuid(node.getUuid());
                clazz.setMappedSyntaxNodePath(node.getPathAsJSONPath());
                clazz.setXCoordinate(node.getXCoordinate());
                clazz.setYCoordinate(node.getYCoordinate());
                if (clazz.getInstance() != null) {
                    clazz.getInstance().setLabel(node.getLabel());
                }
                combined.getSemanticModel().getNodes().add(clazz);
            }
        }
        return combined;
    }
}
