package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.basic.PythonRecommendation;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
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

    private final OntologyApiClient ontologyApiClient;
    private final WebClient client;

    private final List<SemanticModelNode> knownNodes;
    private final List<Relation> knownRelations;

    @Value("${recommendation.service.url}")
    private String recommendationServiceUrl;

    @Value("${recommendation.service.endpoint}")
    private String recommendationServiceEndpoint;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public RecommendationHandler(
            OntologyApiClient ontologyApiClient) {
        this.ontologyApiClient = ontologyApiClient;
        knownNodes = new ArrayList<>();
        knownRelations = new ArrayList<>();

        client = WebClient.builder()
                .baseUrl(recommendationServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CombinedModel generateRecommendations(CombinedModel combinedModel) {
        combinedModel.getRecommendations().clear();
        List<String> positives = combinedModel.getSemanticModel().getNodes().stream()
                .filter(node -> node instanceof Class)
                .map(node -> (Class) node)
                .map(Class::getURI)
                .map(name -> name.replace("plasma:", "http://tmdtkg#"))
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();

        Iterable<SemanticModelNode> targets;

        if (combinedModel.getLastModification() != null && !combinedModel.getLastModification().isDeletion()) {
            targets = combinedModel.getLastModification().getEntities();
        } else {
            targets = combinedModel.getSemanticModel().getNodes();
        }

        targets = combinedModel.getSemanticModel().getNodes();

        for (SemanticModelNode node : targets) {
            // TODO make async
            if (!(node instanceof Class)) {
                // only do for classes, not literals or named entities
                continue;
            }
            String targetURI = node.getURI().replace("plasma:", "http://tmdtkg#");
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

            recommendations.stream()
                    .map(
                            rec -> {

                                ArrayList<SemanticModelNode> entities = new ArrayList<>();
                                ArrayList<Relation> relations = new ArrayList<>();

                                String objectURI = rec.getO().replace("http://tmdtkg#", "plasma:"); // KGS speaks a different language

                                NodeTemplate template = getNodeTemplateForURI(objectURI, combinedModel.getProvisionalElements());
                                if (!(template instanceof SemanticModelNode)) {
                                    return null;
                                }

                                SemanticModelNode objectNode = ((SemanticModelNode) template).instanciate();
                                objectNode.setXCoordinate(node.getXCoordinate() != null ? node.getXCoordinate() : null);
                                objectNode.setYCoordinate(node.getYCoordinate() != null ? node.getYCoordinate() + 150 : null);

                                String relationURI = rec.getP().replace("http://tmdtkg#", "plasma:"); // KGS speaks a different language

                                RelationTemplate relationTemplate = getRelationTemplateForURI(relationURI);

                                if (!(relationTemplate instanceof Relation)) {
                                    return null;
                                }

                                Relation r = ((Relation) relationTemplate).instanciate(node.getUuid(), objectNode.getUuid());
                                entities.add(objectNode);
                                relations.add(r);

                                DeltaModification dm = new DeltaModification("test",
                                        entities,
                                        relations,
                                        new ArrayList<>(),
                                        new ArrayList<>(),
                                        rec.getC());
                                dm.getAnchors().add(node.getUuid());
                                return dm;
                            }
                    )
                    .filter(Objects::nonNull)
                    .forEach(dm -> combinedModel.getRecommendations().add(dm));
        }
        return combinedModel;
    }

    private NodeTemplate getNodeTemplateForURI(String uri, List<SemanticModelNode> provisionalClasses) {
        Optional<SemanticModelNode> clazz = knownNodes.stream()
                .filter(sClass -> sClass.getURI().equals(uri))
                .findFirst();

        if (clazz.isPresent()) {
            return clazz.get().convertToTemplate();
        }

        Optional<SemanticModelNode> existingProvisional = provisionalClasses.stream()
                .filter(pec -> pec.getURI().equals(uri))
                .findFirst();

        if (existingProvisional.isPresent()) {
            return existingProvisional.get().convertToTemplate();
        }

        // try to find in KGS
        Optional<Class> fromKGS = ontologyApiClient.getElements(null, null, uri).stream()
                .filter(element -> element instanceof Class) // TODO add NamedEntities
                .map(element -> (Class) element)
                .findFirst();
        if (fromKGS.isPresent()) {
            Class c = fromKGS.get();
            c.convertToTemplate();
            knownNodes.add(c);
            provisionalClasses.add(c);
            return c;
        }

        // create a new one
        SemanticModelNode newOne = new Class(uri, uri, "").convertToTemplate();
        knownNodes.add(newOne);
        provisionalClasses.add(newOne);
        return newOne;
    }

    private RelationTemplate getRelationTemplateForURI(String uri) {
        Optional<Relation> relationTemplate = knownRelations.stream()
                .filter(rel -> rel.getURI().equals(uri))
                .findFirst();

        if (relationTemplate.isPresent()) {
            return relationTemplate.get();
        }

        // try to find in KGS
        Optional<Relation> fromKGS = ontologyApiClient.getRelations(null, null, uri).stream()
                .findFirst();
        if (fromKGS.isPresent()) {
            Relation r = fromKGS.get();
            r.convertToTemplate();
            knownRelations.add(r);
            return r;
        }

        // TODO handle dataproperties
        ObjectProperty objectProperty = new ObjectProperty("", "", uri);

        return objectProperty.convertToTemplate();
    }
}
