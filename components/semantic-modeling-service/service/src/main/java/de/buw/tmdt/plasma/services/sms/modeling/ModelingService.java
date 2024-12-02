package de.buw.tmdt.plasma.services.sms.modeling;

import de.buw.tmdt.plasma.datamodel.*;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxNode;
import de.buw.tmdt.plasma.services.config.ConfigServiceClient;
import de.buw.tmdt.plasma.services.sms.SemanticModelingService;
import de.buw.tmdt.plasma.services.sms.ars.ARSService;
import de.buw.tmdt.plasma.services.sms.kg.OntologyService;
import de.buw.tmdt.plasma.services.sms.kg.SemanticModelService;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import de.buw.tmdt.plasma.services.sms.modeling.database.ModelingRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModelingService {

    private static final Logger log = LoggerFactory.getLogger(ModelingService.class);

    private final ModelingRepository modelingRepository;

    private final SemanticModelingService semanticModelingService;

    private final ConfigServiceClient configServiceClient;

    private final ARSService arsService;
    private final OntologyService ontologyService;
    private final SemanticModelService semanticModelService;

    @Value("${plasma.gateway_address}")
    private String gateway;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${plasma.sms.feature.finalizemodels.enabled:true}")
    private Boolean finalizeModelsEnabled;

    @Value("${plasma.sms.feature.recommendations.enabled:false}")
    private Boolean recommendationsEnabled;

    @Autowired
    public ModelingService(
            @NotNull ModelingRepository modelingRepository, SemanticModelingService semanticModelingService,
            @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
            ConfigServiceClient configServiceClient, ARSService arsService, OntologyService ontologyService, SemanticModelService semanticModelService) {
        this.modelingRepository = modelingRepository;
        this.semanticModelingService = semanticModelingService;
        this.configServiceClient = configServiceClient;
        this.arsService = arsService;
        this.ontologyService = ontologyService;
        this.semanticModelService = semanticModelService;
    }

    public Modeling initializeModeling(String modelId, CombinedModel combinedModel, String name, String description, String dataId) {
        Optional<Modeling> modelingResult = modelingRepository.findById(modelId);

        if (modelingResult.isPresent()) {
            log.info("Data source already exists");
            return modelingResult.get();
        }
        ZonedDateTime created = ZonedDateTime.now();
        Modeling modeling = new Modeling(modelId, name, description, created, dataId);
        // save initial version of submitted model
        modeling.pushCombinedModel(combinedModel);
        modeling = persistModeling(modeling);
        return modeling;
    }

    public Modeling updateModeling(@NotNull String modelId, @Nullable String name, @Nullable String description, @Nullable String dataId) {
        Modeling modeling = findModelingOrThrow(modelId);
        if (name != null && !name.isBlank()) {
            modeling.setName(name);
        }
        if (description != null && !description.isBlank()) {
            modeling.setDescription(description);
        }
        // dataId update is prohibited for now
        modeling = modelingRepository.save(modeling);
        return modeling;
    }

    public CombinedModel performSemanticLabeling(String modelId) throws ResponseStatusException {
        if (!recommendationsEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Recommendations are currently disabled.");
        }

        Modeling modeling = findModelingOrThrow(modelId);
        try {
            CombinedModel labeledCombinedModel;
            labeledCombinedModel = arsService.performLabeling(
                    modelId, modeling, null, null, modeling.getCurrentModel());
            modeling.pushCombinedModel(labeledCombinedModel);
            // store the model after recommendations have been added
            modeling = persistModeling(modeling);
        } catch (ResponseStatusException e) {
            log.warn("Semantic labeling did not succeed: ", e);
            if (e.getStatus() == HttpStatus.NOT_IMPLEMENTED) {
                throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "No labeling services are defined to process this request.");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during labeling: " + e.getMessage());
        }
        return modeling.getCurrentModel();
    }

    public CombinedModel performSemanticModeling(String modelId) throws ResponseStatusException {
        if (!recommendationsEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Recommendations are currently disabled.");
        }
        Modeling modeling = findModelingOrThrow(modelId);
        try {
            CombinedModel modeledCombinedModel;
            modeledCombinedModel = arsService.performModeling(
                    modelId, modeling, null, null, modeling.getCurrentModel());
            modeling.pushCombinedModel(modeledCombinedModel);
            // store the model after recommendations have been added
            modeling = persistModeling(modeling);

            // ARS
            // uuid
            // model

        } catch (ResponseStatusException e) {
            log.warn("Semantic modeling did not succeed: ", e);
            if (e.getStatus() == HttpStatus.NOT_IMPLEMENTED) {
                throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "No modeling services are defined to process this request.");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during modeling: " + e.getMessage());
        }
        return modeling.getCurrentModel();
    }

    /**
     * Copies the semantic model from the source model into the given model.
     * All uuids are generated new, all mappings or modifications are removed.
     *
     * @param modelId       The target model id
     * @param sourceModelId The source model id
     * @return The updated model
     */
    public CombinedModel copySemanticModelFromOtherModel(@NotNull String modelId, @NotNull String sourceModelId) {
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel model = getCombinedModel(modelId, true);
        CombinedModel sourceCM = getCombinedModel(sourceModelId);

        SemanticModel sourceSM = sourceCM.getSemanticModel();
        if (!sourceCM.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model " + modelId + " is not finalized.");
        }
        DeltaModification delta = new DeltaModification("clone-" + sourceModelId);
        // sanitize and clone all elements
        Map<String, String> uuidMapping = new HashMap<>(); // keep track of exchanged uuids to adjust relations later on
        sourceSM.getNodes().stream().map(node -> {
                    String newUuid = UUID.randomUUID().toString();
                    uuidMapping.put(node.getUuid(), newUuid);
                    if (node instanceof Class) {
                        Class source = (Class) node;
                        return new Class(source.getURI(), source.getLabel(),
                                source.getDescription(), source.getXCoordinate(), source.getYCoordinate(), newUuid);
                    } else if (node instanceof NamedEntity) {
                        NamedEntity source = (NamedEntity) node;
                        return new NamedEntity(source.getURI(), source.getLabel(),
                                source.getDescription(), source.getXCoordinate(), source.getYCoordinate(), newUuid);
                    } else if (node instanceof Literal) {
                        Literal source = (Literal) node;
                        return new Literal(newUuid, source.getValue(), source.getXCoordinate(), source.getYCoordinate());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .forEach(node -> delta.getEntities().add(node));
        sourceSM.getEdges().stream().map(rel -> {
                    String newUuid = UUID.randomUUID().toString();
                    uuidMapping.put(rel.getUuid(), newUuid);
                    if (rel instanceof ObjectProperty) {
                        return new ObjectProperty(newUuid, uuidMapping.get(rel.getFrom()), uuidMapping.get(rel.getTo()),
                                rel.getURI(), rel.getLabel(), rel.getDescription(), null);
                    } else if (rel instanceof DataProperty) {
                        return new DataProperty(newUuid, uuidMapping.get(rel.getFrom()), uuidMapping.get(rel.getTo()),
                                rel.getURI(), rel.getLabel(), rel.getDescription(), null);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .forEach(rel -> delta.getRelations().add(rel));

        model.apply(delta);
        modeling.pushCombinedModel(model);
        persistModeling(modeling);
        return model;
    }

    @SuppressWarnings("SameParameterValue") // this might change
    @NotNull
    private CombinedModel getCombinedModel(@NotNull String modelId, boolean modelWillBeModified) {
        try {
            return getCombinedModel(findModelingOrThrow(modelId), modelWillBeModified);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving combined model for modeling");
        }
    }

    @NotNull
    private CombinedModel getCombinedModel(@NotNull Modeling modeling, boolean modelWillBeModified) {
        CombinedModel currentModel = modeling.getCurrentModel();

        if (modelWillBeModified && currentModel.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalized models are not allowed to be modified");
        }
        return currentModel;
    }

    public @NotNull
    CombinedModel getCombinedModel(@NotNull String modelId) {
        return getCombinedModel(modelId, false);
    }

    /**
     * Returns the existing arraycontexts for a given modeling as a list of sets of strings.
     * Each set in the list represents a single arraycontext and contains all uuids of all relations and nodes in that arraycontext.
     *
     * @param modelId The model id to obtain the information for
     * @return An "index" of arraycontexts, represented as a list of sets of uuids of relations and nodes in that arraycontext.
     */
    @NotNull
    public List<ArrayContext> getArrayContexts(@NotNull String modelId) {
        CombinedModel combinedModel = getCombinedModel(modelId);
        List<ArrayContext> contexts = combinedModel.getArrayContexts().stream()
                .map(sm -> new ArrayContext(
                        sm.getNodes().stream().map(CombinedModelElement::getUuid).collect(Collectors.toList()),
                        sm.getEdges().stream().map(CombinedModelElement::getUuid).collect(Collectors.toList()),
                        null,
                        null,
                        sm.getNodes().stream()
                                .filter(SemanticNode::isMapped)
                                .map(node -> (MappableSemanticNode) node)
                                .map(node -> combinedModel.getSyntaxModel().getNode(node.getMappedSyntaxNodeUuid()))
                                .map(SyntaxNode::getDepth)
                                .max(Long::compare)
                                .orElse(-1L)
                ))
                .collect(Collectors.toList());
        return contexts;
    }

    @NotNull
    public List<ModelMapping> getModelMappings(@NotNull String modelId) {
        CombinedModel combinedModel = getCombinedModel(modelId);
        List<ModelMapping> modelMappings = combinedModel.generateModelMappings();
        return modelMappings;
    }

    /**
     * Retrieves all available {@link SemanticNode}s, from the KGS as well as the current model.
     *
     * @param modelId The modeling uuid
     * @param prefix  A search prefix
     * @param infix   A search infix
     * @return The resulting list of matching {@link SemanticNode}s
     */
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
    @NotNull
    public List<SemanticNode> getElements(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        Modeling modeling = findModelingOrThrow(modelId);
        List<SemanticNode> availableElements;
        try {
            availableElements =
                    ontologyService.getElements(modeling.getSelectedOntologies(), prefix, infix, null);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ontology service currently unavailable.");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findModelingOrThrow(modelId), false);
        combinedModel.getProvisionalElements().stream()
                .filter(element -> {
                    if (prefix != null && !(prefix.isBlank())) {
                        return StringUtils.startsWithIgnoreCase(element.getLabel(), prefix);
                    }
                    if (infix != null && !(infix.isBlank())) {
                        return StringUtils.containsIgnoreCase(element.getLabel(), infix);
                    }
                    return true;
                })
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(element -> availableElements.stream().noneMatch(ae -> ae.sameAs(element)))
                .peek(element -> element.setProvisional(true))
                .forEach(availableElements::add);

        return availableElements.stream()
                .sorted(Comparator.comparing(SemanticNode::getLabel))
                .collect(Collectors.toList());
    }

    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // stream collector does not return null
    @NotNull
    public List<Relation> getRelations(@NotNull String modelId, @Nullable String prefix, @Nullable String infix) {
        Modeling modeling = findModelingOrThrow(modelId);

        List<Relation> availableRelations;
        try {
            availableRelations =
                    ontologyService.getRelations(modeling.getSelectedOntologies(), prefix, infix, null);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ontology service currently unavailable.");
        }

        // Retrieve the concepts that were created within this semantic model...
        CombinedModel combinedModel = getCombinedModel(findModelingOrThrow(modelId), false);
        combinedModel.getProvisionalRelations().stream()
                .filter(relation -> {
                    if (prefix != null && !(prefix.isBlank())) {
                        return StringUtils.startsWithIgnoreCase(relation.getLabel(), prefix);
                    }
                    if (infix != null && !(infix.isBlank())) {
                        return StringUtils.containsIgnoreCase(relation.getLabel(), infix);
                    }
                    return true;
                })
                // ... and add them to the ones from the KGS while ensuring that there are no conflicts
                .filter(relationConcept -> availableRelations.stream().noneMatch(ar -> ar.sameAs(relationConcept)))
                .peek(element -> element.setProvisional(true))
                .forEach(availableRelations::add);

        return availableRelations.stream()
                .sorted(Comparator.comparing(Relation::getLabel))
                .collect(Collectors.toList());
    }

    /* SYNTAX NODE OPERATIONS */

    /**
     * Updates the position of a {@link SyntaxNode} in the {@link CombinedModel}.
     *
     * @param modelId        The modeling uuid identifying the {@link CombinedModel}
     * @param syntaxNode     The {@link SyntaxNode} to place
     * @param schemaNodeUuid The reference uuid of the existing {@link SyntaxNode} to update
     * @deprecated Legacy function, use {@link #applyModification(String, DeltaModification)} instead
     */
    @Deprecated
    public void placeSchemaNode(@NotNull String modelId, @NotNull UUID schemaNodeUuid, @NotNull SyntaxNode syntaxNode) {
        if (!schemaNodeUuid.toString().equals(syntaxNode.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passed uuid in syntax model node is inconsistent to resource to update. "
                            + "Resource: " + schemaNodeUuid + " Payload: " + syntaxNode.getUuid()
            );
        }
        if (syntaxNode.getXCoordinate() == null || syntaxNode.getYCoordinate() == null) {
            StringJoiner stringJoiner = new StringJoiner(" and ", "Missing ", " coordinate in SchemaNode.");
            if (syntaxNode.getXCoordinate() == null) {
                stringJoiner.add("X");
            }
            if (syntaxNode.getYCoordinate() == null) {
                stringJoiner.add("Y");
            }
            //noinspection HardcodedFileSeparator
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    stringJoiner.toString()
            );
        }

        updatePositions(modelId, Collections.singletonList(syntaxNode));
    }


    public void prepareForFrontend(CombinedModel combinedModel) {
    }

    /**
     * Applies all modifications stored in the modification object to the {@link CombinedModel} for the modeling
     * identified by modelId.
     *
     * @param modelId      The modeling uuid identifying the model
     * @param modification The modifications. All modifications are applied in one step
     * @return The updated {@link CombinedModel}
     */
    @NotNull
    public CombinedModel applyModification(@NotNull String modelId, @NotNull DeltaModification modification) {

        //load schema
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);

        CombinedModel clone = combinedModel.copy();
        try {
            clone.apply(modification);
        } catch (Exception e) {
            // something went wrong
            // TODO add more fine grained exceptions
            log.warn("Exception during application of modification: " + modification + ":");
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error during application of modification: " + e.getMessage());
        }
        persistCombinedModel(modeling, clone);

        return clone;
    }

    /**
     * This function updates the positions of the given movedElements.
     * All other changes are ignored, only the position information is updated.
     *
     * @param modelId       The modeling uuid identifying the {@link CombinedModel} to update
     * @param movedElements A list of elements with updated positions, can be syntactic and semantic elements
     */
    public void updatePositions(@NotNull String modelId, @NotNull List<PositionedCombinedModelElement> movedElements) {
        //load referenced modeling
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);

        // create a map for quick lookups
        final Map<String, PositionedCombinedModelElement> elementLookup =
                combinedModel.getSyntaxModel().getNodes().stream()
                        .map(schemaNode -> (PositionedCombinedModelElement) schemaNode)
                        .collect(Collectors.toMap(CombinedModelElement::getUuid, Function.identity()));

        combinedModel.getSemanticModel().getNodes().stream()
                .map(semanticNode -> (PositionedCombinedModelElement) semanticNode)
                .forEach(node -> elementLookup.put(node.getUuid(), node));

        for (PositionedCombinedModelElement movedElement : movedElements) {
            PositionedCombinedModelElement current = elementLookup.get(movedElement.getUuid());
            if (current == null) {
                log.warn("Could not update position for non-existing element in CombinedModel: {}", movedElement.getUuid());
                continue;
            }
            current.setXCoordinate(movedElement.getXCoordinate());
            current.setYCoordinate(movedElement.getYCoordinate());
        }
        persistModeling(modeling);
    }


    @NotNull
    public SemanticNode cacheNode(@NotNull String modelId, @NotNull SemanticNode element) {

        element.validate();

        // validate the URI
        ontologyService.validateURI(element.getURI());

        //load model - DON'T COPY since caching of provisional nodes is not "undoable"
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);

        if (element instanceof Class || element instanceof NamedEntity) {
            final SemanticNode usedElement = getAndUpdateCache(
                    element,
                    n -> Objects.equals(n.getURI(), element.getURI()),
                    combinedModel.getProvisionalElements());

            persistModeling(modeling);
            return usedElement;
        } else {
            log.info("Received unknown type " + element);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a provisional node of this type");
        }
    }

    @NotNull
    public SemanticNode updateCachedNode(@NotNull String modelId, @NotNull SemanticNode node, @Nullable String oldURI) {

        node.validate();

        // validate the new URI
        ontologyService.validateURI(node.getURI());

        //load model - DON'T COPY since caching of provisional nodes is not "undoable"
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);
        if (oldURI == null || oldURI.isBlank()) {
            oldURI = node.getURI();
        }
        String refURI = oldURI;
        if (node instanceof Class || node instanceof NamedEntity) {
            final SemanticNode usedElement = getAndUpdateCache(
                    node,
                    n -> Objects.equals(n.getURI(), refURI),
                    combinedModel.getProvisionalElements());

            combinedModel.getSemanticModel().getNodes().forEach(sm_node -> {
                if (Objects.equals(sm_node.getURI(), refURI)) {
                    sm_node.setURI(node.getURI());
                    sm_node.setLabel(node.getLabel());
                    if (node instanceof Class) {
                        ((Class) sm_node).setDescription(((Class) node).getDescription());
                    }
                }
            });

            persistModeling(modeling);
            return usedElement;
        } else {
            log.info("Received unknown type " + node);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a provisional node of this type");
        }

    }

    public void uncacheNode(@NotNull String modelId, String uri) {
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);
        boolean inUse = combinedModel.getSemanticModel().getNodes().stream().anyMatch(node -> Objects.equals(node.getURI(), uri));
        if (inUse) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Node with URI " + uri + " is in use in this model. Please remove all instances before deleting.");
        }

        boolean change = combinedModel.getProvisionalElements().removeIf(o -> o.getURI().equals(uri));
        if (change) {
            persistModeling(modeling);
        }
    }

    @NotNull
    public Relation cacheRelation(@NotNull String modelId, @NotNull Relation relation) {

        // validate the URI
        ontologyService.validateURI(relation.getURI());

        relation.convertToTemplate();
        relation.validate();

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);
        final Relation usedRelation = getAndUpdateCache(
                relation,
                rel -> Objects.equals(rel.getURI(), relation.getURI()),
                combinedModel.getProvisionalRelations()
        );
        persistModeling(modeling);

        return usedRelation;

    }

    @NotNull
    public Relation updateCachedRelation(@NotNull String modelId, @NotNull Relation relation, @Nullable String oldURI) {

        // validate the URI
        ontologyService.validateURI(relation.getURI());

        relation.convertToTemplate();
        relation.validate();

        //load schema - DON'T COPY since caching of transient concepts is not "undoable"
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);
        if (oldURI == null || oldURI.isBlank()) {
            oldURI = relation.getURI();
        }
        String refURI = oldURI;
        final Relation usedRelation = getAndUpdateCache(
                relation,
                element -> Objects.equals(element.getURI(), refURI),
                combinedModel.getProvisionalRelations()
        );

        combinedModel.getSemanticModel().getEdges().forEach(sm_edge -> {
            if (Objects.equals(sm_edge.getURI(), refURI)) {
                sm_edge.setUri(relation.getURI());
                sm_edge.setLabel(relation.getLabel());
                sm_edge.setDescription(relation.getDescription());
            }
        });

        persistModeling(modeling);

        return usedRelation;

    }

    public void uncacheRelation(@NotNull String modelId, String uri) {
        Modeling modeling = findModelingOrThrow(modelId);
        CombinedModel combinedModel = getCombinedModel(modeling, true);

        boolean inUse = combinedModel.getSemanticModel().getEdges().stream().anyMatch(rel -> Objects.equals(rel.getURI(), uri));
        if (inUse) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relation with URI " + uri + " is in use in this model. Please remove all usages before deleting.");
        }

        boolean change = combinedModel.getProvisionalRelations().removeIf(o -> o.getURI().equals(uri));
        if (change) {
            persistModeling(modeling);
        }
    }

    /**
     * NOTE: this has side effects on the passed list
     * Finds the entry in the cache which is equivalent to {@code element} concerning it's identity function and returns it. If there is none it uses the
     * passed element and adds it to the cache. <i>Equivalence</i> of two elements {@code x} and {@code y} here means:
     * {@code identityFunction.apply(x).equals(identityFunction.apply(y)}
     *
     * @param element            the entry which is looked for
     * @param identifierFunction the function which identifies the element to update
     * @param elementCache       the cache of known entries
     * @return an entity concept which is equivalent to {@code entityConcept}
     */
    @NotNull
    private <T extends CombinedModelElement> T getAndUpdateCache(
            @NotNull T element,
            @NotNull Function<T, Boolean> identifierFunction,
            @NotNull List<T> elementCache
    ) {

        Optional<T> provisionalElement = elementCache.stream()
                .filter(identifierFunction::apply)
                .findAny();
        if (provisionalElement.isPresent()) {
            // delete the old element
            T t = provisionalElement.get();
            elementCache.remove(t);
            element.setUuid(t.getUuid()); // keep the UUID in any case!
        }
        elementCache.add(element);
        return element;
    }

    /* UNDO / REDO Functions */

    public @NotNull
    CombinedModel undoModelModification(@NotNull String modelId) {
        //Get the modeling from the repository
        Modeling modeling = findModelingOrThrow(modelId);
        try {
            modeling.popCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
        persistModeling(modeling);
        return modeling.getCurrentModel();
    }

    public @NotNull
    CombinedModel redoModelModification(@NotNull String modelId) {
        //Get the modeling from the repository
        Modeling modeling = findModelingOrThrow(modelId);
        try {
            modeling.restoreCombinedModel();
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        persistModeling(modeling);
        return modeling.getCurrentModel();
    }

    /* Finish operations */

    /**
     * Finishes the modeling of the given modeling.
     * The semantic model is persisted in the KGS.
     * Provisional concepts are dropped and recommendations removed.
     * After calling this function, the {@link CombinedModel} of the modeling cannot be changed as it is set to finalized.
     * The given {@link CombinedModel} is cloned first, so the change is reversible as a new instance is returned.
     *
     * @param modelId The id of the modeling to finalize
     * @return An updated model instance with the new persistence ids (entityIds) included
     */
    @NotNull
    @Transactional
    public CombinedModel finalizeModeling(@NotNull String modelId) {
        if (!finalizeModelsEnabled) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Model finalization is not allowed" +
                    " in the current configuration.");
        }
        //Load the modeling from the repository.
        Modeling modeling = modelingRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Modeling does not exist"
                ));

        CombinedModel combinedModel = getCombinedModel(modeling, false);

        combinedModel.validate();

        try {
            combinedModel = createSemanticModel(combinedModel, modelId);
        } catch (CombinedModelIntegrityException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred during conversion.");
        }
        // clear provisional elements and recommendations
        combinedModel.getRecommendations().clear();
        combinedModel.getProvisionalRelations().clear();
        combinedModel.getProvisionalElements().clear();

        // prevent any further modifications
        combinedModel.finalizeModel();

        modeling.pushCombinedModel(combinedModel);
        modeling.finalizeModeling(); // indicate the modeling as finalized (replicates the state of the CM)
        Modeling finalModeling = modelingRepository.save(modeling);
        return finalModeling.getCurrentModel();
    }

    /**
     * Clones an existing modeling at its current state.
     * New modeling will have the old name with "(Clone)" appended.
     * All history is deleted for that model and all IDs reset.
     * If the model was finalized, the clone can be edited again.
     *
     * @param modelId The id of the modeling to clone
     * @return A cloned instance of the given modeling with all ids reset and finalized set to false.
     */
    @Transactional
    public Modeling cloneModeling(@NotNull String modelId) {
        //Load the modeling from the repository.
        Modeling modeling = modelingRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Modeling does not exist"
                ));

        CombinedModel combinedModel = getCombinedModel(modeling, false);

        Modeling newModeling = new Modeling(UUID.randomUUID().toString(),
                modeling.getName() + " (Clone)",
                modeling.getDescription(),
                ZonedDateTime.now(),
                UUID.randomUUID().toString());


        CombinedModel copy = combinedModel.copy(); // create a deep copy to decouple from Java objects
        /* Now replace all ids to not cause any unintentional side effects
        (although this should only happen when we mix elements from two models (who would do this?....)
        Current clone version only resets CM and SM ids, may be subject to change!
        */

        CombinedModel clone = new CombinedModel(UUID.randomUUID().toString(),
                copy.getSyntaxModel(),
                copy.getSemanticModel()); // generate a new CM id
        clone.getSemanticModel().setId(null); // for now, we only reset the semantic model id


        newModeling.pushCombinedModel(clone);

        newModeling = modelingRepository.save(newModeling);
        return newModeling;
    }

    /**
     * Sends the {@link SemanticModel} of the given {@link CombinedModel} to the KGS to persist it.
     * ID mappings (the KGS does not use our UUIDs) is done in this function, including the reverse mapping once
     * new KGS ids are generated. Those are then stored in the entityId field.
     * Equal concepts used by multiple entities or relations are merged during this process to not clutter the KG.
     * The given {@link CombinedModel} is cloned first, so the change is reversible as a new instance is returned.
     *
     * @param combinedModel The combined model containing the {@link SemanticModel}
     * @param modelId       The modeling uuid
     * @return The updated {@link CombinedModel}
     * @throws CombinedModelIntegrityException If the integrity of the {@link CombinedModel} is violated during the concept merge
     */
    @NotNull
    private CombinedModel createSemanticModel(@NotNull CombinedModel combinedModel, @NotNull String modelId) throws CombinedModelIntegrityException {

        combinedModel = combinedModel.copy();
        SemanticModel semanticModel = combinedModel.getSemanticModel();

        //send request
        final SemanticModel response;
        try {
            log.info("Creating Semantic Model");
            if (semanticModel.getId() == null) {
                // just use the one from the combined model
                semanticModel.setId(modelId);
            }
            response = semanticModelService.createSemanticModel(semanticModel);
            log.info("Created Semantic Model");
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when creating semantic model in the KGS", e);
        }

        combinedModel = new CombinedModel(modelId, combinedModel.getSyntaxModel(), response, null, true);

        return combinedModel;
    }

    @Transactional
    public void deleteModel(@NotNull String modelId) {
        Optional<Modeling> possibleSchema = modelingRepository.findById(modelId);
        if (possibleSchema.isPresent()) {
            modelingRepository.delete(possibleSchema.get());
        } else {
            log.warn("Model with id {} was not available", modelId);
        }
    }

    @SuppressWarnings("unused")
    @Transactional
    public void persistCombinedModel(@NotNull String modelId, @NotNull CombinedModel combinedModel) {
        Modeling modeling = findModelingOrThrow(modelId);
        persistCombinedModel(modeling, combinedModel);
    }

    @Transactional
    public void persistCombinedModel(@NotNull Modeling modeling, @NotNull CombinedModel combinedModel) {
        modeling.pushCombinedModel(combinedModel);
        modelingRepository.save(modeling);
    }

    @Transactional
    public Modeling persistModeling(@NotNull Modeling modeling) {
        return modelingRepository.save(modeling);
    }

    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // throws Exception if null
    @NotNull
    public Modeling findModelingOrThrow(@NotNull String modelId) {
        return modelingRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid modeling id: " + modelId));
    }

    @NotNull
    public CombinedModel requestRecommendations(@NotNull String modelId, @NotNull CombinedModel combinedModel) {
        Modeling modeling = findModelingOrThrow(modelId);
        if (recommendationsEnabled) {
            try {
                return arsService.performRefinement(modelId, modeling, null, null, combinedModel);
            } catch (Exception e) {
                log.warn("Could not reach SRS for recommendations");
            }
        }
        return combinedModel;
    }

    public List<String> getSelectedOntologies(@NotNull String modelId) {
        Modeling modeling = findModelingOrThrow(modelId);
        return modeling.getSelectedOntologies();
    }

    public void updateSelectedOntologies(@NotNull String modelId, @NotNull List<String> selectedOntologies) {
        Modeling modeling = findModelingOrThrow(modelId);
        modeling.getSelectedOntologies().clear();
        modeling.getSelectedOntologies().addAll(selectedOntologies);
        persistModeling(modeling);
    }

    /***
     * update the preferred services
     @param modelId ID of the Model to be updated
     @param preferredServices new List
     @author Sven Stopka
     */
    public void updatePreferredServices(@NotNull String modelId, @NotNull PreferredServices preferredServices) {
        Modeling modeling = findModelingOrThrow(modelId);
        modeling.setPreferredLabelingService(preferredServices.getPreferredLabeling());
        modeling.setPreferredModelingService(preferredServices.getPreferredModeling());
        modeling.setPreferredRefiningService(preferredServices.getPreferredRefinement());
        persistModeling(modeling);
    }

    public PreferredServices getPreferredServices(@NotNull String modelId) {
        Modeling modeling = findModelingOrThrow(modelId);
        return new PreferredServices(
                modeling.getPreferredLabelingService(),
                modeling.getPreferredModelingService(),
                modeling.getPreferredRefiningService()
        );
    }
}
