package de.buw.tmdt.plasma.ars.labeling.lm.service.handler;

import de.buw.tmdt.plasma.ars.labeling.lm.service.converter.KGS2CMConverter;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.PrimitiveEntityTypeMapping;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.Edge;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.config.ConfigServiceClient;
import de.buw.tmdt.plasma.services.kgs.shared.api.UniversalKnowledgeApi;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LabelMatchingHandler {

    private static final Logger log = LoggerFactory.getLogger(LabelMatchingHandler.class);

    final UniversalKnowledgeApi universalKnowledgeApi;

    private ConfigServiceClient configClient;

    public LabelMatchingHandler(UniversalKnowledgeApi universalKnowledgeApi,
                                @NotNull ConfigServiceClient configService) {
        this.universalKnowledgeApi = universalKnowledgeApi;
        this.configClient = configService;
    }

    public CombinedModel performLabeling(String dataSourceUUID, String configId, String configToken, CombinedModel combined) {

        SyntaxModel syntaxModelDTO = combined.getSyntaxModel();
        for (SchemaNode node : syntaxModelDTO.getNodes()) {
            if (!(node instanceof PrimitiveNode)) {
                continue;
            }

            String label = node.getLabel();
            Collection<EntityConceptDTO> kgsEntityConcepts = universalKnowledgeApi.getEntityConcepts(label, null);
            Collection<EntityConcept> entityConcepts = kgsEntityConcepts.stream()
                    .map(KGS2CMConverter::convert)
                    .collect(Collectors.toList());

            Optional<EntityConcept> matching = entityConcepts.stream().filter(entityConceptDTO -> entityConceptDTO.getLabel().equals(label)).findFirst();
            if (matching.isPresent()) {  // we only match on exact equality
                // we found one
                // create an EntityType
                EntityType et = new EntityType(node.getLabel(),
                        "",
                        matching.get());
                et.setOriginalLabel(node.getLabel());
                et.setSyntaxNodeUuid(node.getUuid());
                combined.getSemanticModel().getNodes().add(et);
                // add a mapping from ET to node
                Optional<Edge> firstEdge = syntaxModelDTO.getEdges().stream().filter(edgeDTO -> edgeDTO.getToId().equals(node.getUuid())).findFirst();
                if (firstEdge.isPresent()) {
                    // we got an edge, find where it comes from
                    Edge edge = firstEdge.get();
                    Optional<SchemaNode> parentNode = syntaxModelDTO.getNodes().stream().filter(schemaNodeDTO -> schemaNodeDTO.getUuid().equals(edge.getFromId())).findFirst();
                    if (parentNode.isPresent()) {
                        PrimitiveEntityTypeMapping mapping = new PrimitiveEntityTypeMapping(
                                node.getUuid(),
                                et.getUuid(),
                                parentNode.get().getUuid());

                        combined.getPrimitiveEntityTypeMappings().add(mapping);
                        log.info("Assigned EC '{}' to node '{}'", matching.get().getLabel(), node.getLabel());
                    } else {
                        log.info("Could not assign EC '{}' to node '{}'. No parent found", matching.get().getLabel(), node.getLabel());
                    }
                } else {
                    log.info("Could not assign EC '{}' to node '{}'. No incoming edge found", matching.get().getLabel(), node.getLabel());
                }

            }
        }
        return combined;
    }
}
