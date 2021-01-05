package de.buw.tmdt.plasma.ars.modeling.sm.service.handler;

import de.buw.tmdt.plasma.ars.modeling.sm.service.basic.SchemaAnalysisSemanticLabeling;
import de.buw.tmdt.plasma.ars.modeling.sm.service.converter.RuleDTOConverter;
import de.buw.tmdt.plasma.ars.modeling.sm.service.converter.SchemaAnalysisDTOConverter;
import de.buw.tmdt.plasma.ars.modeling.sm.service.converter.SemanticModelDTOConverter;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Analysis;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.EntityRule;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.RelationRule;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Standard;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.semanticmodel.*;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.syntaxmodel.Node;
import de.buw.tmdt.plasma.ars.modeling.sm.service.repository.AnalysisRepository;
import de.buw.tmdt.plasma.ars.modeling.sm.service.repository.StandardRepository;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.syntaxmodel.NodeDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.RelationConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.UniversalKnowledgeApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandardMatchingHandler {

    private final RuleDTOConverter ruleDTOConverter;
    private final SchemaAnalysisDTOConverter schemaAnalysisDTOConverter;
    private final SemanticModelDTOConverter semanticModelDTOConverter;
    private final UniversalKnowledgeApiClient universalKnowledgeApiClient;
    private final AnalysisRepository analysisRepository;
    private final StandardRepository standardRepository;

    public StandardMatchingHandler(RuleDTOConverter ruleDTOConverter,
                                   SchemaAnalysisDTOConverter schemaAnalysisDTOConverter,
                                   SemanticModelDTOConverter semanticModelDTOConverter,
                                   UniversalKnowledgeApiClient universalKnowledgeApiClient,
                                   AnalysisRepository analysisRepository,
                                   StandardRepository standardRepository) {
        this.ruleDTOConverter = ruleDTOConverter;
        this.schemaAnalysisDTOConverter = schemaAnalysisDTOConverter;
        this.semanticModelDTOConverter = semanticModelDTOConverter;
        this.universalKnowledgeApiClient = universalKnowledgeApiClient;
        this.analysisRepository = analysisRepository;
        this.standardRepository = standardRepository;
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
     * Create a semantic model using a syntax model and the uuid of the standard to apply.
     *
     * @param syntaxModelDTO syntax model dto, as provided by the sas
     * @param standardId     uuid of the standard to use
     * @return dto of the semantic model
     */
    public SemanticModelDTO modelWithStandard(NodeDTO syntaxModelDTO, String standardId) {

        // Find the provided standard to apply
        Standard standard =
                standardRepository.findById(UUID.fromString(standardId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standard ID not found"));

        Node syntaxModel = schemaAnalysisDTOConverter.fromNodeDTO(syntaxModelDTO);
        SemanticModel semanticModel = generateSemanticModel(syntaxModel, standard);

        // Create a new analysis object to make sure that database ids are existing
        Analysis analysis = new Analysis(UUID.randomUUID());
        analysis.setSemanticModel(semanticModel);
        // load and parse the saved model including ids
        analysis = analysisRepository.save(analysis);
        return semanticModelDTOConverter.toDTO(analysis.getSemanticModel());
    }

    /**
     * Returns the semantic model for a given syntax model and standard.
     *
     * @param syntaxModel The original syntax model as created by SAS
     * @param standard    The selected standard
     * @return semantic model with applied standard
     */
    public SemanticModel generateSemanticModel(Node syntaxModel, Standard standard) {

        SchemaAnalysisSemanticLabeling schemaAnalysisSemanticLabeling = new SchemaAnalysisSemanticLabeling();

        Map<String, String> requiredPredecessors = new HashMap<>();
        Map<String, RelationConcept> relationConceptMap = new HashMap<>();
        Map<String, EntityConcept> entityConceptMap = new HashMap<>();

        Analysis analysis = new Analysis(UUID.randomUUID());
        analysis.setResult(syntaxModel);
        analysis.setStandard(standard);

        if (analysis.getStandard() != null) {
            schemaAnalysisSemanticLabeling.setFixMappings(getSemanticLabelingByRules(analysis, requiredPredecessors, entityConceptMap));
        }

        schemaAnalysisSemanticLabeling.setRequiredPredecessors(requiredPredecessors);


        try {
            schemaAnalysisSemanticLabeling.execute(syntaxModel);
        } catch (SchemaAnalysisException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        Map<String, List<String>> history = schemaAnalysisSemanticLabeling.getNodeHistory();
        List<EntityType> entityTypes = schemaAnalysisSemanticLabeling.getEntityTypes();
        List<Relation> relations = new ArrayList<>();

        if (analysis.getStandard() != null) {
            modelByRules(analysis, entityTypes, relations, requiredPredecessors, history, entityConceptMap, relationConceptMap);
        }

        return new SemanticModel(null, "", "", entityTypes, relations, null);

    }


    /**
     * Performs semantic labeling by rules.
     *
     * @param analysis             the analysis object to select the standard from
     * @param requiredPredecessors map defining which predecessors certain labels need before assigning them
     * @param entityConceptMap     map of labels to entity concepts
     * @return map of labels and mapped concepts
     */
    private Map<String, EntityConcept> getSemanticLabelingByRules(Analysis analysis, Map<String, String> requiredPredecessors,
                                                                  Map<String, EntityConcept> entityConceptMap) {

        List<EntityRule> labelRules =
                analysis.getStandard().getEntityRules();

        Map<String, EntityConcept> fixConcepts = new HashMap<>();

        for (EntityRule entityRule : labelRules) {
            if (entityRule.getOriginalAttribute() != null) {
                if (!entityConceptMap.containsKey(entityRule.getConceptName())) {

                    String uuid = UUID.randomUUID().toString();

                    for (EntityConceptDTO entityConcept : universalKnowledgeApiClient.getEntityConcepts(entityRule.getConceptName())) {
                        if (entityConcept.getMainLabel().equals(entityRule.getConceptName()) &&
                                entityConcept.getDescription().equals(entityRule.getConceptDescription())) {
                            uuid = entityConcept.getId();
                        }
                    }
                    EntityConcept entityConcept = new EntityConcept(uuid, entityRule.getConceptName(), entityRule.getConceptDescription(), entityRule.getConceptName()); // Use concept name as uri
                    entityConceptMap.put(entityRule.getConceptName(), entityConcept);
                }

                fixConcepts.put(entityRule.getOriginalAttribute(), entityConceptMap.get(entityRule.getConceptName()));

            }
            requiredPredecessors.put(entityRule.getConceptName(), entityRule.getRequiredPredecessor());
        }

        return fixConcepts;
    }


    /**
     * Perform semantic modeling by rules.
     *
     * @param analysis             analysis object to pick standard from
     * @param entityTypes          list of all used entity types
     * @param relations            list of all used relations
     * @param requiredPredecessors map defining which predecessors certain labels need before assigning them
     * @param history              map defining the paths to each node
     * @param entityConceptMap     map of labels to entity concepts
     * @param relationConceptMap   map of relation concepts
     */
    private void modelByRules(Analysis analysis,
                              List<EntityType> entityTypes,
                              List<Relation> relations,
                              Map<String, String> requiredPredecessors,
                              Map<String, List<String>> history,
                              Map<String, EntityConcept> entityConceptMap,
                              Map<String, RelationConcept> relationConceptMap) {

        List<String> allAvailableTypes = new ArrayList<>();

        for (Map.Entry<String, List<String>> h : history.entrySet()) {
            allAvailableTypes.addAll(h.getValue());
        }

        Set<String> allTypes = new HashSet<>(allAvailableTypes);
        allTypes.add(null);

        List<EntityRule> list2 = analysis.getStandard().getEntityRules().stream().filter(x -> x.getOriginalAttribute() == null).collect(Collectors.toList());
        List<RelationRule> list3 = analysis.getStandard().getRelationRules();
        List<RelationRule> foundRelations = new ArrayList<>();


        boolean actionFound = true;

        while (actionFound) {
            actionFound = false;
            for (RelationRule relationRule : list3) {
                if (foundRelations.contains(relationRule)) {
                    continue;
                }
                List<EntityType> candidatesFrom = entityTypes
                        .stream().filter(x -> x.getEntityConcept().getName().equals(relationRule.getFromConcept())).collect(Collectors.toList());

                List<EntityType> candidatesTo = entityTypes
                        .stream().filter(x -> x.getEntityConcept().getName().equals(relationRule.getToConcept())).collect(Collectors.toList());

                if (candidatesFrom.size() == 0) {
                    List<EntityRule> candidates = list2
                            .stream().filter(x -> x.getConceptName().equals(relationRule.getFromConcept())).collect(Collectors.toList());

                    if (candidates.size() > 0) {
                        for (EntityRule candidate : candidates) {
                            if (!entityConceptMap.containsKey(candidate.getConceptName())) {
                                String uuid = UUID.randomUUID().toString();

                                for (EntityConceptDTO entityConcept : universalKnowledgeApiClient.getEntityConcepts(candidate.getConceptName())) {
                                    if (entityConcept.getMainLabel().equals(candidate.getConceptName()) &&
                                            entityConcept.getDescription().equals(candidate.getConceptDescription())) {
                                        uuid = entityConcept.getId();
                                        break;
                                    }
                                }
                                EntityConcept entityConcept = new EntityConcept(uuid, candidate.getConceptName(),
                                        candidate.getConceptDescription(), candidate.getConceptName() // Use concept name as uri
                                );
                                entityConceptMap.put(candidate.getConceptName(), entityConcept);
                            }
                            EntityConcept entityConcept = entityConceptMap.get(candidate.getConceptName());

                            EntityType entityType = new EntityType(null, entityConcept.getName(), entityConcept.getName(), entityConcept.getDescription(),
                                    entityConcept, null
                            );
                            if (allTypes.contains(requiredPredecessors.get(entityConcept.getName()))) {
                                entityTypes.add(entityType);
                                candidatesFrom.add(entityType);
                            }
                        }
                    }
                }

                if (candidatesTo.size() == 0) {
                    List<EntityRule> candidates = list2
                            .stream().filter(x -> x.getConceptName().equals(relationRule.getToConcept())).collect(Collectors.toList());

                    if (candidates.size() > 0) {
                        for (EntityRule candidate : candidates) {
                            if (allTypes.contains(candidate.getRequiredPredecessor())) {

                                if (!entityConceptMap.containsKey(candidate.getConceptName())) {

                                    String uuid = UUID.randomUUID().toString();

                                    for (EntityConceptDTO entityConcept : universalKnowledgeApiClient.getEntityConcepts(candidate.getConceptName())) {
                                        if (entityConcept.getMainLabel().equals(candidate.getConceptName()) &&
                                                entityConcept.getDescription().equals(candidate.getConceptDescription())) {
                                            uuid = entityConcept.getId();
                                            break;
                                        }
                                    }
                                    EntityConcept entityConcept = new EntityConcept(uuid, candidate.getConceptName(), candidate.getConceptDescription(), candidate.getConceptName()); // Use concept name as uri
                                    entityConceptMap.put(candidate.getConceptName(), entityConcept);
                                }
                                EntityConcept entityConcept = entityConceptMap.get(candidate.getConceptName());

                                EntityType entityType = new EntityType(null, entityConcept.getName(), entityConcept.getName(), entityConcept.getDescription(),
                                        entityConcept, null
                                );
                                if (allTypes.contains(requiredPredecessors.get(entityConcept.getName()))) {
                                    entityTypes.add(entityType);
                                    candidatesTo.add(entityType);
                                }
                            }
                        }
                    }
                }

                if (candidatesFrom.size() > 0 && candidatesTo.size() > 0) {
                    actionFound = true;

                    if (!relationConceptMap.containsKey(relationRule.getConceptName())) {

                        String uuid = UUID.randomUUID().toString();

                        for (RelationConceptDTO entityConcept : universalKnowledgeApiClient.getRelationConcepts(relationRule.getConceptName())) {
                            if (entityConcept.getLabel().equals(relationRule.getConceptName()) &&
                                    entityConcept.getDescription().equals(relationRule.getConceptDescription())) {
                                uuid = entityConcept.getId();
                                break;
                            }
                        }

                        RelationConcept relationConcept = new RelationConcept(
                                uuid,
                                relationRule.getConceptName(),
                                relationRule.getConceptDescription(),
                                relationRule.getConceptName(), // Use concept name as uri
                                new HashSet<>(),
                                null
                        );
                        relationConceptMap.put(relationRule.getConceptName(), relationConcept);
                    }


                    RelationConcept relationConcept = relationConceptMap.get(relationRule.getConceptName());

                    for (EntityType fromCandidate : candidatesFrom) {
                        for (EntityType toCandidate : candidatesTo) {
                            Relation r = new Relation(null, fromCandidate, toCandidate, relationConcept, relationConcept.getDescription(), null);
                            relations.add(r);
                            foundRelations.add(relationRule);
                        }
                    }
                }
            }
        }
    }
}
