package de.buw.tmdt.plasma.services.kgs.scheduling;

import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriverFactory;
import de.buw.tmdt.plasma.services.kgs.database.neo4j.Neo4JModel;
import de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.EntityConcept;
import de.buw.tmdt.plasma.services.sds.shared.dto.concept.SDConceptDTO;
import de.buw.tmdt.plasma.services.sds.shared.dto.request.RequestDTO;
import de.buw.tmdt.plasma.services.sds.shared.feignclient.SemanticDatabaseClient;
import de.buw.tmdt.plasma.utilities.misc.Pair;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.buw.tmdt.plasma.services.sds.shared.dto.Database.DATAMUSE;
import static de.buw.tmdt.plasma.services.sds.shared.dto.Database.WORDNET;
import static de.buw.tmdt.plasma.utilities.collections.ArrayUtilities.toCollection;
import static de.buw.tmdt.plasma.utilities.collections.CollectionUtilities.map;

@Component
public class ConnectSynonymConcepts {
    private static final Logger logger = LoggerFactory.getLogger(ConnectSynonymConcepts.class);
    private final GraphDBDriverFactory graphDBDriverFactory;
    private final SemanticDatabaseClient semanticDatabaseClient;

    @Autowired
    public ConnectSynonymConcepts(
            @NotNull GraphDBDriverFactory graphDBDriverFactory,
            @NotNull SemanticDatabaseClient semanticDatabaseClient) {
        this.graphDBDriverFactory = graphDBDriverFactory;
        this.semanticDatabaseClient = semanticDatabaseClient;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void validateSynonyms() {
        logger.info("Validating the synonym relationships inside the graph through external data sources.");
        //Get all EC
        final Set<EntityConcept> initiallyExistingConcepts = graphDBDriverFactory.getGraphDBDriver().retrieveEntityConceptsByPrefix("");
        //Retrieve all Synonyms from the semantic database service
        final Map<String, Set<SDConceptDTO>> responseData;
        try {
            responseData = semanticDatabaseClient.resultSynonyms(new RequestDTO(
                    map(initiallyExistingConcepts, EntityConcept::getMainLabel, HashSet::new),
                    toCollection(HashSet::new, WORDNET)
            )).getResultMap().entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey(), map(entry.getValue(), Function.identity(), HashSet::new)))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        } catch (FeignException e) {
            logger.warn("Could not reach Semantic Database Service when querying synonyms - aborting", e);
            return;
        }
        final Map<String, Set<SDConceptDTO>> datamuseData;
        try {
            datamuseData = getSynonymsDatamuseConcepts(map(
                    initiallyExistingConcepts,
                    EntityConcept::getMainLabel,
                    HashSet::new
            ));
        } catch (FeignException e) {
            logger.warn("Could not reach Semantic Database Service when querying data muse - aborting", e);
            return;
        }
        for (EntityConcept graphConcept : initiallyExistingConcepts) {
            final String ecLabel = graphConcept.getMainLabel();
            final Set<String> datamuseSyns = datamuseData.get(ecLabel).stream().map(SDConceptDTO::getLabel).collect(Collectors.toSet());
            final Set<SDConceptDTO> retrievedSynonymConcepts = responseData.get(ecLabel);
            datamuseSyns.retainAll(Stream.concat(
                    retrievedSynonymConcepts.stream().map(SDConceptDTO::getLabel),
                    retrievedSynonymConcepts.stream().map(SDConceptDTO::getSynonyms).flatMap(Set::stream)
            ).collect(Collectors.toSet()));
            Set<EntityConcept> synsInGraph = new HashSet<>();
            datamuseSyns.forEach(syn -> synsInGraph.addAll(graphDBDriverFactory
                    .getGraphDBDriver()
                    .retrieveEntityConceptsByPrefix(syn.toLowerCase().replaceAll(
                            "_",
                            " "
                    ))));
            if (!synsInGraph.isEmpty()) {
                for (EntityConcept syn : synsInGraph) {
                    Set<String> synonyms = graphDBDriverFactory.getGraphDBDriver().retrieveEntityConceptSynonyms(syn.getId());
                    if (!syn.getId().equals(graphConcept.getId()) && !synonyms.contains(graphConcept.getMainLabel())
                            && !hasEdge(graphConcept.getId(), syn.getId())) {
                        graphDBDriverFactory.getGraphDBDriver().createEdge(graphConcept.getId(), syn.getId(), Neo4JModel.EntityConcept.Edge.SYNONYM);
                    }
                }
            }
        }
    }

    private boolean hasEdge(@NotNull String ecID1, @NotNull String ecID2) {
        return graphDBDriverFactory.getGraphDBDriver().hasEdge(ecID1, ecID2, Neo4JModel.EntityConcept.Edge.SYNONYM);
    }

    @NotNull
    private Map<String, Set<SDConceptDTO>> getSynonymsDatamuseConcepts(@NotNull Set<String> labels) throws FeignException {
        return semanticDatabaseClient
                .resultSynonyms(new RequestDTO(
                        labels,
                        toCollection(HashSet::new, DATAMUSE)
                ))
                .getResultMap()
                .entrySet()
                .stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        new HashSet<>(entry.getValue())
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}