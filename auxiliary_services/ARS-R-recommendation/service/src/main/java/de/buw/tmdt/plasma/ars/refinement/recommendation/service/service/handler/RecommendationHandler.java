package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.basic.PythonRecommendation;
import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.converter.KnowledgeGraphServiceDTO2CombinedModelConverter;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.OntologyApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationHandler {

    private final OntologyApiClient universalKnowledgeApiClient;
    private final KnowledgeGraphServiceDTO2CombinedModelConverter kgsDTO2CMConverter;
    private final WebClient client;

    private final List<EntityConcept> knownConcepts;
    private final List<RelationConcept> knownRelations;

    @Value("${recommendation.service.url}")
    private String recommendationServiceUrl;

    @Value("${recommendation.service.endpoint}")
    private String recommendationServiceEndpoint;

    public RecommendationHandler(
            OntologyApiClient universalKnowledgeApiClient,
            KnowledgeGraphServiceDTO2CombinedModelConverter kgsDTO2CMConverter) {
        this.kgsDTO2CMConverter = kgsDTO2CMConverter;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        knownConcepts = new ArrayList<>();
        knownRelations = new ArrayList<>();

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

        Iterable<EntityType> targets;

        if (combinedModel.getLastModification() != null && !combinedModel.getLastModification().isDeletion()) {
            targets = combinedModel.getLastModification().getEntities();
        } else {
            targets = combinedModel.getSemanticModel().getNodes();
        }

        targets = combinedModel.getSemanticModel().getNodes();

        for (EntityType et : targets) {
            // TODO make async
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

                        String objectURI = rec.getO().replace("http://tmdtkg#", "plasma:"); // KGS speaks a different language

                        EntityConcept objectEntityConcept = getEntityConceptForURI(objectURI, combinedModel.getProvisionalEntityConcepts());
                        if (objectEntityConcept.getEntityId() == null) {
                            // this one is not yet known, add it to the provisional entity concepts
                            combinedModel.getProvisionalEntityConcepts().add(objectEntityConcept);
                        }
                        EntityType objectEntityType = new EntityType(objectEntityConcept.getLabel(), "", objectEntityConcept);

                        objectEntityType.setXCoordinate(et.getXCoordinate() != null ? et.getXCoordinate() : null);
                        objectEntityType.setYCoordinate(et.getYCoordinate() != null ? et.getYCoordinate() + 150 : null);

                        String relationURI = rec.getP().replace("http://tmdtkg#", "plasma:"); // KGS speaks a different language

                        RelationConcept rc = getRelationConceptForURI(relationURI);
                        if (rc.getEntityId() == null) {
                            // this one is not yet known, add it to the provisional relation concepts
                            combinedModel.getProvisionalRelationConcepts().add(rc);
                        }
                        Relation r = new Relation(et.getUuid(), objectEntityType.getUuid(), rc);

                        entities.add(objectEntityType);
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

    private EntityConcept getEntityConceptForURI(String uri, List<EntityConcept> provisionalEntityConcepts) {
        Optional<EntityConcept> entityConcept = knownConcepts.stream()
                .filter(ec -> ec.getSourceURI().equals(uri))
                .findFirst();

        if (entityConcept.isPresent()) {
            return entityConcept.get();
        }

        // try to find in KGS
        Optional<EntityConcept> fromKGS = universalKnowledgeApiClient.getEntityConcepts(null, uri).stream()
                .map(kgsDTO2CMConverter::convert)
                .findFirst();
        if (fromKGS.isPresent()) {
            knownConcepts.add(fromKGS.get());
            return fromKGS.get();
        }

        Optional<EntityConcept> existingProvisional = provisionalEntityConcepts.stream().filter(pec -> pec.getSourceURI().equals(uri)).findFirst();
        if (existingProvisional.isPresent()) {
            return existingProvisional.get();
        }
        // create a new one
        return new EntityConcept(uri, "", uri);
    }

    private RelationConcept getRelationConceptForURI(String uri) {
        Optional<RelationConcept> relationConcept = knownRelations.stream()
                .filter(rc -> rc.getSourceURI().equals(uri))
                .findFirst();

        if (relationConcept.isPresent()) {
            return relationConcept.get();
        }

        // try to find in KGS
        Optional<RelationConcept> fromKGS = universalKnowledgeApiClient.getRelationConcepts(null, uri).stream()
                .map(kgsDTO2CMConverter::convert)
                .findFirst();
        if (fromKGS.isPresent()) {
            knownRelations.add(fromKGS.get());
            return fromKGS.get();
        }

        return new RelationConcept(uri, "", uri);
    }
}
