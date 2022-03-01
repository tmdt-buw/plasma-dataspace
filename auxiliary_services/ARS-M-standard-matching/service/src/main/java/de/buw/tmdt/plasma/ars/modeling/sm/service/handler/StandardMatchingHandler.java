package de.buw.tmdt.plasma.ars.modeling.sm.service.handler;

import de.buw.tmdt.plasma.ars.modeling.sm.service.basic.SchemaAnalysisSemanticLabeling;
import de.buw.tmdt.plasma.ars.modeling.sm.service.converter.RuleDTOConverter;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Analysis;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.EntityRule;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.RelationRule;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Standard;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.ars.modeling.sm.service.repository.AnalysisRepository;
import de.buw.tmdt.plasma.ars.modeling.sm.service.repository.StandardRepository;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.services.config.ConfigServiceClient;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.OntologyApiClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandardMatchingHandler {

    private static final Logger log = LoggerFactory.getLogger(StandardMatchingHandler.class);

    private final RuleDTOConverter ruleDTOConverter;
    private final OntologyApiClient universalKnowledgeApiClient;
    private final AnalysisRepository analysisRepository;
    private final StandardRepository standardRepository;
    private final ConfigServiceClient configServiceClient;

    public StandardMatchingHandler(RuleDTOConverter ruleDTOConverter,
                                   @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
                                           OntologyApiClient universalKnowledgeApiClient,
                                   AnalysisRepository analysisRepository,
                                   StandardRepository standardRepository,
                                   ConfigServiceClient configServiceClient) {
        this.ruleDTOConverter = ruleDTOConverter;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        this.analysisRepository = analysisRepository;
        this.standardRepository = standardRepository;
        this.configServiceClient = configServiceClient;
    }


    /**
     * Creates a new standard.
     *
     * @param standardProvisionDTO dto containing necessary information for standard creation
     * @return uuid of the new created standard
     */
    public String createStandard(StandardProvisionDTO standardProvisionDTO) {

        Standard standard;

        standard = new Standard(
                standardProvisionDTO.getName(),
                standardProvisionDTO.getDescription(),
                standardProvisionDTO.getEntityRules().stream().map(ruleDTOConverter::fromDTO).collect(Collectors.toList()),
                standardProvisionDTO.getRelationRules().stream().map(ruleDTOConverter::fromDTO).collect(Collectors.toList())
        );

        standard = standardRepository.save(standard);

        return standard.getUuid().toString();
    }

    /**
     * Returns a list of all stored standards.
     *
     * @return list of standards
     */
    public List<StandardDTO> getStandards() {
        List<Standard> standards = standardRepository.findAll();
        return standards.stream().map(s -> new StandardDTO(s.getUuid().toString(), s.getName(), s.getDescription())).collect(Collectors.toList());
    }

    /**
     * Create a semantic model using a syntax model and the id of the standard to apply.
     *
     * @param combinedModel combined model containing the syntactic model
     * @param configId      id of the config for this task
     * @param configToken   Token to retrieve the config
     * @return dto of the semantic model
     */
    public String modelWithStandard(CombinedModel combinedModel, String configId, String configToken, String analysisId) {

        Map<String, String> config = configServiceClient.getConfig(configId, configToken);
        log.info("Received config: {}", config);

        String standardId = String.valueOf(config.get("STANDARD_ID"));
        if (standardId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid standard id has to be provided as the \"STANDARD_ID\" field in the given config. Was \"" + standardId + "\" or could not be retrieved at all.");
        }

        // Find the provided standard to apply
        Standard standard =
                standardRepository.findById(UUID.fromString(standardId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standard ID not found"));

        CombinedModel resultingModel = generateSemanticModel(combinedModel, standard);

        // Create a new analysis object to make sure that database ids are existing
        Analysis analysis = new Analysis(analysisId);
        analysis.setInput(combinedModel);
        analysis.setResult(resultingModel);

        // load and parse the saved model including ids
        analysisRepository.save(analysis);
        return analysisId;
    }


    /**
     * Returns the semantic model for a given syntax model and standard.
     *
     * @param inputModel CombinedModel containing the original syntax model as created by SAS
     * @param standard   The selected standard
     * @return semantic model with applied standard
     */
    public CombinedModel generateSemanticModel(@NotNull CombinedModel inputModel, @NotNull Standard standard) {

        Analysis analysis = new Analysis(inputModel.getId());
        analysis.setInput(inputModel);
        analysis.setStandard(standard);

        CombinedModel model = inputModel.copy();

        // Map concept sourceURI -> requiredPredecessor
        Map<String, String> requiredPredecessors = new HashMap<>();

        // map attributeLabel -> class
        Map<String, Class> attributeBoundEntityConceptMap = generateAttributeBoundEntityConcepts(standard, requiredPredecessors);

        // perform the labeling
        SchemaAnalysisSemanticLabeling schemaAnalysisSemanticLabeling = new SchemaAnalysisSemanticLabeling();

        schemaAnalysisSemanticLabeling.setFixedRulebasedMappings(attributeBoundEntityConceptMap);

        schemaAnalysisSemanticLabeling.setRequiredPredecessors(requiredPredecessors);

        SemanticModel semanticModel;

        try {
            schemaAnalysisSemanticLabeling.execute(model);
            semanticModel = schemaAnalysisSemanticLabeling.getSemanticModel();
        } catch (SchemaAnalysisException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // generate a modification and add the computed changes to the combined model
        DeltaModification labelingModification = new DeltaModification(
                "semantic_labeling",
                new HashSet<>(semanticModel.getNodes()),
                new HashSet<>(semanticModel.getEdges()),
                null,
                null
        );
        model.apply(labelingModification); // this will also create all needed auxiliary mappings

        Map<String, List<String>> nodePaths = schemaAnalysisSemanticLabeling.getNodePaths();

        // perform semantic modeling
        // map sourceURI -> concept
        Map<String, Class> unboundEntityConceptMap = generateUnboundClasses(standard, requiredPredecessors);

        // Map sourceURI -> concept
        Map<String, Relation> relationConceptMap = generateRelationConcepts(standard);

        try {
            semanticModel = performSemanticModelingByRules(analysis, model, requiredPredecessors, nodePaths, unboundEntityConceptMap, relationConceptMap);
        } catch (SchemaAnalysisException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // generate a modification and add the computed changes to the combined model
        DeltaModification modelingModification = new DeltaModification(
                "semantic_modeling",
                new HashSet<>(semanticModel.getNodes()),
                new HashSet<>(semanticModel.getEdges()),
                null,
                null
        );
        model.apply(modelingModification); // this will also create all needed auxiliary mappings
        return model;
    }

    /**
     * Generates the mappings from attributes to concepts as defined by the standard used in the analysis.
     *
     * @param standard             The standard to obtain the rules from
     * @param requiredPredecessors map defining which predecessors certain labels need before assigning them
     * @return map of labels and mapped concepts
     */
    private Map<String, Class> generateAttributeBoundEntityConcepts(@NotNull Standard standard, Map<String, String> requiredPredecessors) {

        Map<String, Class> fixedRulebasedMappings = new HashMap<>();

        for (EntityRule entityRule : standard.getEntityRules()) {
            if (entityRule.getOriginalAttribute() != null) {
                if (!fixedRulebasedMappings.containsKey(entityRule.getConceptURI())) {
                    Class clazz = getOrGenerateClass(entityRule);
                    fixedRulebasedMappings.put(entityRule.getOriginalAttribute(), clazz);
                }
            }
            requiredPredecessors.put(entityRule.getConceptURI(), entityRule.getRequiredPredecessor());
        }
        return fixedRulebasedMappings;
    }

    /**
     * Generates the unbound concepts as defined by the standard used in the analysis.
     *
     * @param standard             The standard to obtain the rules from
     * @param requiredPredecessors map defining which predecessors certain labels need before assigning them
     * @return map of sourceURIs to associated concepts
     */
    private Map<String, Class> generateUnboundClasses(@NotNull Standard standard, Map<String, String> requiredPredecessors) {


        Map<String, Class> unboundRuleBasedMappings = new HashMap<>();

        for (EntityRule entityRule : standard.getEntityRules()) {
            if (entityRule.getOriginalAttribute() == null || entityRule.getOriginalAttribute().isEmpty()) {
                if (!unboundRuleBasedMappings.containsKey(entityRule.getConceptURI())) {
                    Class entityConcept = getOrGenerateClass(entityRule);
                    unboundRuleBasedMappings.put(entityRule.getConceptURI(), entityConcept);
                }
            }
            requiredPredecessors.put(entityRule.getConceptURI(), entityRule.getRequiredPredecessor());
        }
        return unboundRuleBasedMappings;
    }

    /**
     * Generates the unbound relation concepts as defined by the standard used in the analysis.
     *
     * @param standard The standard to obtain the rules from
     * @return map of sourceURIs to associated relation concepts
     */
    private Map<String, Relation> generateRelationConcepts(@NotNull Standard standard) {


        Map<String, Relation> relationConcepts = new HashMap<>();

        for (RelationRule relationRule : standard.getRelationRules()) {
            if (!relationConcepts.containsKey(relationRule.getRelationURI())) {
                Relation relationConcept = getOrGenerateRelationConcept(relationRule);
                relationConcepts.put(relationRule.getRelationURI(), relationConcept);
            }
        }
        return relationConcepts;
    }

    private Class getOrGenerateClass(EntityRule rule) {
        return universalKnowledgeApiClient.getElements(null, null, rule.getConceptName()).stream()
                .filter(element -> element instanceof Class)
                .map(element -> (Class) element)
                .filter(kgsClass -> kgsClass.getURI().equals(rule.getConceptURI()))
                .findFirst()
                .orElse(
                        (Class) new Class(
                                rule.getConceptURI(),
                                rule.getConceptName(),
                                rule.getConceptDescription())
                                .convertToTemplate()

                );
    }

    private Relation getOrGenerateRelationConcept(RelationRule rule) {
        return universalKnowledgeApiClient.getRelations(null, null, rule.getConceptName()).stream()
                .filter(relation -> relation.getURI().equals(rule.getRelationURI()))
                .findFirst()
                .orElse(new ObjectProperty(
                        rule.getConceptName(),
                        rule.getConceptDescription(),
                        rule.getRelationURI()
                ));
    }


    /**
     * Perform semantic modeling by rules.
     *
     * @param analysis             analysis object to pick standard from
     * @param combinedModel        The input from the labeling step
     * @param requiredPredecessors map defining which predecessors certain labels need before assigning them
     * @param nodePaths            map defining the paths to each node
     * @param unboundClasses       map of sourURIs to entity concepts for non-primitive attached concepts
     * @param relationConceptMap   map of sourceURI to relation concepts
     * @return The generated semantic model containing the newly created entities and relations. For internal use only.
     */
    private SemanticModel performSemanticModelingByRules(Analysis analysis,
                                                         CombinedModel combinedModel,
                                                         Map<String, String> requiredPredecessors,
                                                         Map<String, List<String>> nodePaths,
                                                         Map<String, Class> unboundClasses,
                                                         Map<String, Relation> relationConceptMap) {

        List<String> syntaxNodeNamesAndTokens = nodePaths.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());

        syntaxNodeNamesAndTokens.add(null);

        // only for local use
        SemanticModel semanticModel = combinedModel.getSemanticModel();

        List<EntityRule> nonPrimitiveEntityRules = analysis.getStandard().getEntityRules().stream().filter(x -> x.getOriginalAttribute() == null).collect(Collectors.toList());
        List<RelationRule> relationRules = analysis.getStandard().getRelationRules();
        List<RelationRule> foundRelations = new ArrayList<>();

        boolean actionFound = true;

        while (actionFound) {
            actionFound = false;
            for (RelationRule relationRule : relationRules) {
                if (foundRelations.contains(relationRule)) {
                    continue;
                }
                List<SemanticModelNode> candidatesFrom = semanticModel.getNodes()
                        .stream().filter(x -> x.getURI().equals(relationRule.getFromConceptSourceURI())).collect(Collectors.toList());

                List<SemanticModelNode> candidatesTo = semanticModel.getNodes()
                        .stream().filter(x -> x.getURI().equals(relationRule.getToConceptURI())).collect(Collectors.toList());

                if (candidatesFrom.isEmpty()) {
                    List<EntityRule> candidates = nonPrimitiveEntityRules
                            .stream().filter(x -> x.getConceptURI().equals(relationRule.getFromConceptSourceURI())).collect(Collectors.toList());

                    for (EntityRule candidate : candidates) {
                        if (!unboundClasses.containsKey(candidate.getConceptURI())) {
                            // we have no concept for this
                            continue;
                        }
                        SemanticModelNode clazz = unboundClasses.get(candidate.getConceptURI()).instanciate();

                        if (syntaxNodeNamesAndTokens.contains(requiredPredecessors.get(clazz.getURI()))) {
                            semanticModel.getNodes().add(clazz);
                            candidatesFrom.add(clazz);
                        }
                    }
                }

                if (candidatesTo.isEmpty()) {
                    List<EntityRule> candidates = nonPrimitiveEntityRules
                            .stream().filter(x -> x.getConceptName().equals(relationRule.getToConceptURI())).collect(Collectors.toList());

                    if (!candidates.isEmpty()) {
                        for (EntityRule candidate : candidates) {
                            if (syntaxNodeNamesAndTokens.contains(candidate.getRequiredPredecessor())) {

                                SemanticModelNode clazz = unboundClasses.get(candidate.getConceptURI()).instanciate();
                                if (clazz == null) {
                                    // does not exist
                                    continue;
                                }

                                if (syntaxNodeNamesAndTokens.contains(requiredPredecessors.get(clazz.getURI()))) {
                                    semanticModel.getNodes().add(clazz);
                                    candidatesTo.add(clazz);
                                }
                            }
                        }
                    }
                }

                if (!candidatesFrom.isEmpty() && !candidatesTo.isEmpty()) {
                    actionFound = true;
                    if (!relationConceptMap.containsKey(relationRule.getRelationURI())) {
                        // no match, something is wrong here...
                        continue;
                    }
                    Relation relation = relationConceptMap.get(relationRule.getRelationURI());

                    for (SemanticModelNode fromCandidate : candidatesFrom) {
                        for (SemanticModelNode toCandidate : candidatesTo) {
                            Relation r = relation.instanciate(fromCandidate.getUuid(), toCandidate.getUuid());
                            semanticModel.getEdges().add(r);
                            foundRelations.add(relationRule);
                        }
                    }
                }
            }
        }
        return semanticModel;
    }

    public CombinedModel fetchModel(String dataSourceUuid) {
        Analysis analysis = analysisRepository.findById(dataSourceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Given ID not found"));
        CombinedModel combinedModel = analysis.getResult();
        if (combinedModel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not yet present");
        }
        return combinedModel;

    }
}
