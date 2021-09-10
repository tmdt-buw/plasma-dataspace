package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.basic.PythonRecommendation;
import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.converter.KnowledgeGraphServiceDTO2CombinedModelConverter;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.UniversalKnowledgeApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationHandler {

    private final UniversalKnowledgeApiClient universalKnowledgeApiClient;
    private final KnowledgeGraphServiceDTO2CombinedModelConverter kgsDTO2CMConverter;
    private WebClient client;

    @Value("${recommendation.service.url}")
    private String recommendationServiceUrl;

    @Value("${recommendation.service.endpoint}")
    private String recommendationServiceEndpoint;

    public RecommendationHandler(
            UniversalKnowledgeApiClient universalKnowledgeApiClient,
            KnowledgeGraphServiceDTO2CombinedModelConverter kgsDTO2CMConverter) {
        this.kgsDTO2CMConverter = kgsDTO2CMConverter;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;

        client = WebClient.builder()
                .baseUrl(recommendationServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CombinedModel generateRecommendations(CombinedModel combinedModel) {
        combinedModel.getRecommendations().clear();
        List<String> positives = combinedModel.getSemanticModel().getNodes().stream()
                .map(EntityType::getEntityConcept)
                .map(EntityConcept::getSourceURI)
                .map(name -> name.replace("plasma:", "http://tmdtkg#"))
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();


        for (EntityType et : combinedModel.getSemanticModel().getNodes()) {

            String targetURI = et.getEntityConcept().getSourceURI().replace("plasma:", "http://tmdtkg#");
            Object[] results = client.post()
                    .uri(recommendationServiceUrl, uriBuilder -> uriBuilder
                            .path(recommendationServiceEndpoint)
                            .queryParam("target", targetURI)
                            .build())
                    .bodyValue(positives)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .retrieve()
                    .bodyToMono(Object[].class).log()
                    .block();

            if (results == null) {
                results = new Object[0];
            }

            List<PythonRecommendation> recommendations = Arrays.stream(results)
                    .map(object -> mapper.convertValue(object, PythonRecommendation.class))
                    .collect(Collectors.toList());

            recommendations.stream().map(
                    rec -> {

                        Set<EntityType> entities = new HashSet<>();
                        Set<Relation> relations = new HashSet<>();

                        EntityConcept o_ec = new EntityConcept(rec.getO(), "", rec.getO());
                        EntityType o_et = new EntityType(rec.getO(), "", o_ec);

                        o_et.setXCoordinate(et.getXCoordinate() != null ? et.getXCoordinate() : null);
                        o_et.setYCoordinate(et.getYCoordinate() != null ? et.getYCoordinate() - 150 : null);

                        RelationConcept rc = new RelationConcept(rec.getP(), "", rec.getP());
                        Relation r = new Relation(et.getUuid(), o_et.getUuid(), rc);

                        entities.add(o_et);
                        relations.add(r);

                        DeltaModification dm = new DeltaModification("test",
                                entities,
                                relations,
                                new HashSet<>(),
                                new HashSet<>(),
                                rec.getC());
                        dm.getAnchors().add(et.getUuid());
                        return dm;
                    }
            ).forEach(dm -> combinedModel.getRecommendations().add(dm));
        }
        return combinedModel;
    }
}
