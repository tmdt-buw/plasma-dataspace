package de.buw.tmdt.plasma.ars.labeling.lm.service.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SchemaNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.config.ConfigServiceClient;
import de.buw.tmdt.plasma.services.kgs.shared.api.OntologyApi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
public class LabelMatchingHandler {

    private static final Logger log = LoggerFactory.getLogger(LabelMatchingHandler.class);

    final OntologyApi ontologyApi;

    private ConfigServiceClient configClient;

    public LabelMatchingHandler(OntologyApi ontologyApi,
                                @NotNull ConfigServiceClient configService) {
        this.ontologyApi = ontologyApi;
        this.configClient = configService;
    }

    public CombinedModel performLabeling(String dataSourceUUID, String configId, String configToken, CombinedModel combined) {

        SyntaxModel syntaxModelDTO = combined.getSyntaxModel();
        for (SchemaNode node : syntaxModelDTO.getNodes()) {
            if (!(node instanceof PrimitiveNode)) {
                continue;
            }

            String label = node.getLabel();
            Collection<SemanticModelNode> candidates = ontologyApi.getElements(null,label, null, null);
            List<Class> classes = candidates.stream()
                    .filter(c -> c instanceof Class)
                    .map(c -> (Class) c)
                    .collect(Collectors.toList());


            Optional<Class> matching = classes.stream().filter(entityConceptDTO -> entityConceptDTO.getLabel().equals(label)).findFirst();
            if (matching.isPresent()) {  // we only match on exact equality
                // we found one
                // create an Instance
                Class clazz = matching.get();
                clazz.createInstance();
                clazz.setMappedSyntaxNodeLabel(node.getLabel());
                clazz.setMappedSyntaxNodeUuid(node.getUuid());
                clazz.setMappedSyntaxNodePath(String.join("->", node.getPath()));
                if (clazz.getInstance() != null) {
                    clazz.getInstance().setLabel(node.getLabel());

                }

                combined.getSemanticModel().getNodes().add(clazz);
            }
        }
        return combined;
    }
}
