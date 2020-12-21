package de.buw.tmdt.plasma.services.kgs.scheduling;

import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriverFactory;
import de.buw.tmdt.plasma.services.kgs.database.neo4j.Neo4JModel;
import de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.EntityConcept;
import de.buw.tmdt.plasma.services.sds.shared.dto.concept.SDConceptDTO;
import de.buw.tmdt.plasma.services.sds.shared.dto.request.RequestDTO;
import de.buw.tmdt.plasma.services.sds.shared.feignclient.SemanticDatabaseClient;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.buw.tmdt.plasma.services.sds.shared.dto.Database.DATAMUSE;
import static de.buw.tmdt.plasma.services.sds.shared.dto.Database.WORDNET;
import static de.buw.tmdt.plasma.utilities.collections.ArrayUtilities.toCollection;

@Component
public class ConnectRelatedConcepts {
    private static final Logger logger = LoggerFactory.getLogger(ConnectRelatedConcepts.class);

    private final GraphDBDriverFactory graphDBDriverFactory;
    private final SemanticDatabaseClient semanticDatabaseClient;

    @Autowired
    public ConnectRelatedConcepts(
            @NotNull GraphDBDriverFactory graphDBDriverFactory,
            @NotNull SemanticDatabaseClient semanticDatabaseClient
    ) {
        this.graphDBDriverFactory = graphDBDriverFactory;
        this.semanticDatabaseClient = semanticDatabaseClient;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void validateRelations() {
        Set<EntityConcept> allConceptsInGraph = graphDBDriverFactory.getGraphDBDriver().retrieveEntityConceptsByPrefix("");

        for (EntityConcept ecI : allConceptsInGraph) {

            Set<EntityConcept> remainingEc = new HashSet<>(allConceptsInGraph);
            remainingEc.remove(ecI);

            Set<SDConceptDTO> relatedConceptsForEcI;
            try {
                relatedConceptsForEcI = getRelatedConcepts(ecI.getMainLabel());
            } catch (FeignException e) {
                logger.warn("Could not reach Semantic Database Service - aborting", e);
                return;
            }

            Set<String> relatedConceptNamesForEcI;
            try {
                relatedConceptNamesForEcI = semanticDatabaseClient
                        .resultSynonyms(new RequestDTO(
                                Collections.singleton(ecI.getMainLabel().toLowerCase()),
                                toCollection(HashSet::new, DATAMUSE)
                        ))
                        .getResultMap()
                        .get(ecI.getMainLabel().toLowerCase()).stream()
                        .map(SDConceptDTO::getLabel).collect(Collectors.toSet());
            } catch (FeignException e) {
                logger.warn("Could not reach Semantic Database Service when querying synonyms - aborting", e);
                return;
            }

            //As datamuse works with strings only, a mapping has to be done from the concepts of Babel-/WordNet
            relatedConceptNamesForEcI.retainAll(Stream.concat(
                    relatedConceptsForEcI.stream().map(SDConceptDTO::getLabel),
                    relatedConceptsForEcI.stream()
                            .map(SDConceptDTO::getSynonyms)
                            .flatMap(Set::stream)
            ).collect(Collectors.toSet()));

            for (EntityConcept ecII : remainingEc) {

                if (hasPath(ecI.getId(), ecII.getId())) {

                    if (!hasEdge(ecI.getId(), ecII.getId())) {

                        if (relatedConceptNamesForEcI.contains(ecII.getMainLabel())) {
                            createEdge(ecI.getId(), ecII.getId());
                        }
                    }
                } else {

                    Set<String> relatedConceptsForEcII;
                    try {
                        relatedConceptsForEcII = semanticDatabaseClient
                                .resultSynonyms(new RequestDTO(
                                        Collections.singleton(ecII.getMainLabel().toLowerCase()),
                                        toCollection(HashSet::new, DATAMUSE)
                                ))
                                .getResultMap()
                                .get(ecII.getMainLabel().toLowerCase()).stream()
                                .map(SDConceptDTO::getLabel)
                                .collect(Collectors.toSet());
                    } catch (FeignException e) {
                        logger.warn("Could not reach Semantic Database service - aborting");
                        return;
                    }

                    if (relatedConceptNamesForEcI.contains(ecII.getMainLabel())) {
                        createEdge(ecI.getId(), ecII.getId());
                        continue;
                    }

                    Set<String> commonNeighbors;
                    try {
                        commonNeighbors = semanticDatabaseClient
                                .resultSynonyms(new RequestDTO(
                                        Collections.singleton(ecI.getMainLabel().toLowerCase()),
                                        toCollection(HashSet::new, DATAMUSE)
                                ))
                                .getResultMap()
                                .get(ecI.getMainLabel().toLowerCase()).stream()
                                .map(SDConceptDTO::getLabel)
                                .collect(Collectors.toSet());
                    } catch (FeignException e) {
                        logger.warn("Could not reach Semantic Database service - aborting");
                        return;
                    }

                    commonNeighbors.retainAll(relatedConceptsForEcII);

                    for (String neighbor : commonNeighbors) {

                        Set<EntityConcept> neighborECs = graphDBDriverFactory.getGraphDBDriver().retrieveEntityConceptsByPrefix(neighbor);

                        if (neighborECs.isEmpty()) {
                            EntityConcept newEC = graphDBDriverFactory.getGraphDBDriver().createEntityConcept(
                                    new EntityConcept(
                                            "0",
                                            neighbor,
                                            "",
                                            DATAMUSE.name()
                                    ));

                            neighborECs.add(newEC);
                        }

                        for (EntityConcept ec : neighborECs) {
                            createEdge(ecI.getId(), ec.getId());
                            createEdge(ecII.getId(), ec.getId());
                        }
                    }
                }
            }
        }
    }

    @NotNull
    private Set<SDConceptDTO> getRelatedConcepts(@NotNull String label) throws FeignException {
        return semanticDatabaseClient
                .resultSynonyms(new RequestDTO(
                        Collections.singleton(label),
                        toCollection(HashSet::new, WORDNET)
                ))
                .getResultMap()
                .get(label).stream()
                .map(SDConceptDTO::getRelatedConcepts)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private boolean hasEdge(@NotNull String ecID1, @NotNull String ecID2) {
        return graphDBDriverFactory.getGraphDBDriver().hasEdge(ecID1, ecID2, Neo4JModel.EntityConcept.Edge.RELATED);
    }

    private void createEdge(@NotNull String ecIDfrom, @NotNull String ecIDto) {
        graphDBDriverFactory.getGraphDBDriver().createEdge(ecIDfrom, ecIDto, Neo4JModel.EntityConcept.Edge.RELATED);
    }

    private boolean hasPath(@NotNull String ecIDfrom, @NotNull String ecIDto) {
        return graphDBDriverFactory.getGraphDBDriver().hasPath(ecIDfrom, ecIDto);
    }
}
