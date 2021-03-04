package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.*;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.Pair;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The combined model contains all elements that model the current state of the syntactic
 * and semantic model of a data source.
 */
public class CombinedModel implements Serializable {

    public static final String DATA_SOURCE_UUID = "dataSourceUuid";
    public static final String SYNTAX_MODEL_PROPERTY = "syntaxModel";
    public static final String SEMANTIC_MODEL_PROPERTY = "semanticModel";
    private static final String PRIMITIVE_ENTITY_TYPE_LIST = "primitiveToEntityTypeList";
    private static final String RECOMMENDATION_LIST = "recommendations";
    private static final String FINALIZED = "finalized";
    private static final String PROVISIONAL_ENTITY_CONCEPTS_PROPERTY = "provisionalEntityConcepts";
    private static final String PROVISIONAL_RELATION_CONCEPTS_PROPERTY = "provisionalRelationConcepts";
    private static final long serialVersionUID = 1069167546126071914L;


    private final String dataSourceUuid;
    private final SyntaxModel syntaxModel;
    private final SemanticModel semanticModel;
    /**
     * Stores provisional {@link EntityConcept}s that the user created but are not yet in the knowledge base.
     * Only valid for this model.
     */
    @NotNull
    private final List<EntityConcept> provisionalEntityConcepts;

    /**
     * Stores provisional {@link RelationConcept}s that the user created but are not yet in the knowledge base.
     * Only valid for this model.
     */
    @NotNull
    private final List<RelationConcept> provisionalRelationConcepts;

    /**
     * Supplementary mappings which are used to render edges from syntax nodes to entity types that overshadow syntax nodes.
     * Only used for rendering purposes in the frontend.
     * Will be updated automatically if {@link #apply(DeltaModification)} is called.
     */
    @NotNull
    private final ArrayList<PrimitiveEntityTypeMapping> primitiveEntityTypeMappings;

    /**
     * Recommendations represent suggested modifications for the current state of models.
     */
    @NotNull
    private ArrayList<DeltaModification> recommendations;

    /**
     * A finalized model should not be changed any more.
     */
    private boolean finalized;

    public CombinedModel(
            @NotNull String dataSourceUuid,
            @Nullable SyntaxModel syntaxModel,
            @Nullable SemanticModel semanticModel
    ) {
        this(dataSourceUuid, syntaxModel, semanticModel, null, null, false);
    }

    public CombinedModel(
            @NotNull String dataSourceUuid,
            @Nullable SyntaxModel syntaxModel,
            SemanticModel semanticModel,
            List<PrimitiveEntityTypeMapping> primitiveEntityTypeMappings,
            List<DeltaModification> recommendations,
            boolean finalized
    ) {
        this(dataSourceUuid, syntaxModel, semanticModel, primitiveEntityTypeMappings, recommendations, finalized, null, null);

    }

    @JsonCreator
    public CombinedModel(
            @JsonProperty(DATA_SOURCE_UUID) @NotNull String dataSourceUuid,
            @JsonProperty(SYNTAX_MODEL_PROPERTY) @Nullable SyntaxModel syntaxModel,
            @JsonProperty(SEMANTIC_MODEL_PROPERTY) SemanticModel semanticModel,
            @JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST) List<PrimitiveEntityTypeMapping> primitiveEntityTypeMappings,
            @JsonProperty(RECOMMENDATION_LIST) List<DeltaModification> recommendations,
            @JsonProperty(value = FINALIZED, defaultValue = "false") boolean finalized,
            @JsonProperty(PROVISIONAL_ENTITY_CONCEPTS_PROPERTY) List<EntityConcept> provisionalEntityConcepts,
            @JsonProperty(PROVISIONAL_RELATION_CONCEPTS_PROPERTY) List<RelationConcept> provisionalRelationConcepts

    ) {
        this.dataSourceUuid = dataSourceUuid;
        this.syntaxModel = syntaxModel == null ? new SyntaxModel() : syntaxModel;
        this.semanticModel = semanticModel == null ? new SemanticModel() : semanticModel;
        this.primitiveEntityTypeMappings = primitiveEntityTypeMappings == null ? new ArrayList<>() : new ArrayList<>(primitiveEntityTypeMappings);
        this.finalized = finalized;
        this.recommendations = recommendations == null ? new ArrayList<>() : new ArrayList<>(recommendations);
        this.provisionalEntityConcepts = provisionalEntityConcepts == null ? new ArrayList<>() : provisionalEntityConcepts;
        this.provisionalRelationConcepts = provisionalRelationConcepts == null ? new ArrayList<>() : provisionalRelationConcepts;
    }

    @JsonProperty(DATA_SOURCE_UUID)
    public String getDataSourceUuid() {
        return dataSourceUuid;
    }

    @JsonProperty(SYNTAX_MODEL_PROPERTY)
    public SyntaxModel getSyntaxModel() {
        return syntaxModel;
    }

    @JsonProperty(SEMANTIC_MODEL_PROPERTY)
    public SemanticModel getSemanticModel() {
        return semanticModel;
    }

    @JsonProperty(value = FINALIZED, defaultValue = "false")
    public boolean isFinalized() {
        return finalized;
    }

    @JsonProperty(PRIMITIVE_ENTITY_TYPE_LIST)
    public List<PrimitiveEntityTypeMapping> getPrimitiveEntityTypeMappings() {
        return primitiveEntityTypeMappings;
    }

    @NotNull
    @JsonProperty(RECOMMENDATION_LIST)
    public ArrayList<DeltaModification> getRecommendations() {
        return recommendations;
    }

    public void finalizeModel() {
        this.finalized = true;
    }

    // we allow later injections of recommendations, will be solved with new data model
    public void setRecommendations(@NotNull ArrayList<DeltaModification> recommendations) {
        this.recommendations = recommendations;
    }

    /**
     * Returns all provisional {@link EntityConcept}s in this model.
     *
     * @return A list of provisional {@link EntityConcept}s in the model
     */
    @NotNull
    @JsonProperty(PROVISIONAL_ENTITY_CONCEPTS_PROPERTY)
    public List<EntityConcept> getProvisionalEntityConcepts() {
        return provisionalEntityConcepts;
    }

    /**
     * Returns all provisional {@link RelationConcept}s in this model.
     *
     * @return A list of provisional {@link RelationConcept}s in the model
     */
    @NotNull
    @JsonProperty(PROVISIONAL_RELATION_CONCEPTS_PROPERTY)
    public List<RelationConcept> getProvisionalRelationConcepts() {
        return provisionalRelationConcepts;
    }

    /**
     * Main method to modify the combined model.
     * {@link DeltaModification}s can contain multiple changes at one time and usually define the "wanted" state
     * with this method performing the necessary (subsequent) adjustments to the model.
     * If any modifications are made to the model outside of this function, consistency is not guaranteed.
     * NOTE: This function does not create a copy of the model. In case of an exception being thrown,
     * the state of the model is undefined and may be inconsistent.
     *
     * @param delta The modification to apply
     */
    public void apply(DeltaModification delta) throws CombinedModelIntegrityException {
        if (finalized || syntaxModel == null) {
            throw new UnsupportedOperationException("Model is finalized. No more modifications possible");
        }
        // check if this modification contains any inconsistencies
        validateModification(delta);

        // apply syntactic modifications
        // update nodes
        List<SchemaNode> nodesToBeReplaced = new ArrayList<>();
        if (CollectionUtilities.hasElements(delta.getNodes())) {
            if (delta.isDeletion()) {
                throw new UnsupportedOperationException("Cannot remove syntax nodes");
            }
            // as we match by uuid only, we can use removeAll and addAll
            // remove all nodes that are to be overwritten (i.e. uuids match)
            nodesToBeReplaced = syntaxModel.getNodes().stream().filter(schemaNode -> delta.getNodes().contains(schemaNode)).collect(Collectors.toList());
            syntaxModel.getNodes().removeAll(nodesToBeReplaced);


            // now add the new ones with the same uuids but new content
            syntaxModel.getNodes().addAll(delta.getNodes());
        }
        // update edges
        if (CollectionUtilities.hasElements(delta.getEdges())) {
            if (delta.isDeletion()) {
                throw new UnsupportedOperationException("Cannot remove syntax nodes");
            }
            // as we match by uuid only, we can use removeAll and addAll
            // remove all edges that are to be overwritten (i.e. uuids match)
            syntaxModel.getEdges().removeAll(delta.getEdges());
            // now add the new ones with the same uuids but new content
            syntaxModel.getEdges().addAll(delta.getEdges());
        }

        // check if any additional syntactic modifications have to be done, e.g. after splitting operation
        if (!delta.isDeletion()) { // cannot delete from syntax model
            // check if he have a split going on
            List<CompositeNode> compositeNodes = delta.getNodes().stream().filter(schemaNode -> schemaNode instanceof CompositeNode).map(schemaNode -> (CompositeNode) schemaNode).collect(Collectors.toList());
            for (CompositeNode compositeNode : compositeNodes) {
                // get the replaced node
                SchemaNode replacedNode = nodesToBeReplaced.stream().filter(schemaNode -> schemaNode.equals(compositeNode)).findFirst().orElse(null);
                if (replacedNode == null) {
                    // nothing to be done here, something has gone wrong
                    continue;
                }
                if (replacedNode instanceof CompositeNode) {
                    // this was split before. we change the splitting patterns
                    CompositeNode replacedCompositeNode = (CompositeNode) replacedNode;
                    ArrayList<Pair<Edge, PrimitiveNode>> newComponents = new ArrayList<>(compositeNode.getSplitter().size() + 1);
                    // modify child nodes of the composite node
                    // find outgoing edges and construct map of edge -> target node
                    List<Pair<Edge, PrimitiveNode>> children = getSyntaxModel().getEdges().stream()
                            .filter(edge -> edge.getFromId().equals(replacedCompositeNode.getUuid()))
                            .map(edge -> Pair.of(edge, getSyntaxModel().getNodes().stream()
                                    .filter(node -> edge.getToId().equals(node.getUuid()))
                                    .map(node -> (PrimitiveNode) node)
                                    .findFirst().orElseThrow()))
                            .collect(Collectors.toList());

                    // recycle old components or create new ones if not enough existing
                    for (int i = 0; i < compositeNode.getSplitter().size() + 1; i++) {
                        if (i < children.size()) {
                            Pair<Edge, PrimitiveNode> pair = children.get(i);
                            pair.getRight().getExamples().clear(); // will be filled again later
                            newComponents.add(pair);
                        } else {
                            PrimitiveNode primitiveNode = new PrimitiveNode(
                                    "split" + "-" + i,
                                    new ArrayList<>(),
                                    true,
                                    DataType.Unknown,
                                    new ArrayList<>(),
                                    null);
                            newComponents.add(Pair.of(new Edge(compositeNode.getUuid(), primitiveNode.getUuid()), primitiveNode));
                        }
                    }

                    // split and distribute the example values again
                    for (String example : replacedCompositeNode.getExamples()) {
                        String leftOver = example;
                        for (int i = 0; i < compositeNode.getSplitter().size(); i++) {
                            Splitting splitter = compositeNode.getSplitter().get(i);
                            Pair<String, String> split = splitter.apply(leftOver);
                            // annotate the edge with the splitting pattern
                            newComponents.get(i).getLeft().setAnnotation(splitter.getPattern());
                            newComponents.get(i).getRight().getExamples().add(split.getLeft());
                            leftOver = split.getRight();
                        }
                        newComponents.get(newComponents.size() - 1).getRight().getExamples().add(leftOver);
                    }

                    List<PrimitiveNode> newNodes = newComponents.stream().map(Pair::getRight).collect(Collectors.toList());
                    // remove all old nodes
                    children.stream().map(Pair::getRight).forEach(syntaxModel.getNodes()::remove);
                    // now add newNodes (restoring some or all of the old components to the model)
                    syntaxModel.getNodes().addAll(newNodes);

                    // similarly, update the edges
                    List<Edge> newEdges = newComponents.stream().map(Pair::getLeft).collect(Collectors.toList());
                    children.stream().map(Pair::getLeft).forEach(syntaxModel.getEdges()::remove);
                    syntaxModel.getEdges().addAll(newEdges);

                } else if (replacedNode instanceof PrimitiveNode) {
                    // we split a previous primitive node
                    PrimitiveNode replaced = (PrimitiveNode) replacedNode;
                    // copy existing example values, cleansing pattern and coordinates to composite node
                    compositeNode.setExamples(replaced.getExamples());
                    compositeNode.setXCoordinate(replaced.getXCoordinate());
                    compositeNode.setYCoordinate(replaced.getYCoordinate());
                    compositeNode.setCleansingPattern(replaced.getCleansingPattern());

                    List<Splitting> splitter = compositeNode.getSplitter();

                    // generate new nodes
                    List<Pair<Edge, PrimitiveNode>> newComponents = new ArrayList<>(splitter.size() + 1);
                    for (int i = 0; i < splitter.size() + 1; i++) {
                        PrimitiveNode newNode = new PrimitiveNode(
                                replaced.getLabel() + "-" + i,
                                replaced.getOperations(),
                                true,
                                ((PrimitiveNode) replacedNode).getDataType(),
                                new ArrayList<>(),
                                null);
                        Edge newEdge = new Edge(compositeNode.getUuid(), newNode.getUuid());
                        newComponents.add(Pair.of(newEdge, newNode));
                    }

                    // split example values
                    for (String example : replaced.getExamples()) {

                        String leftOver = example;
                        for (int i = 0; i < splitter.size(); i++) {
                            Splitting splitting = splitter.get(i);

                            Pair<String, String> split = splitter.get(i).apply(leftOver);
                            newComponents.get(i).getRight().getExamples().add(split.getLeft());
                            newComponents.get(i).getLeft().setAnnotation(splitting.getPattern());
                            leftOver = split.getRight();
                        }
                        newComponents.get(newComponents.size() - 1).getRight().getExamples().add(leftOver);
                    }

                    List<PrimitiveNode> newNodes = newComponents.stream().map(Pair::getRight).collect(Collectors.toList());
                    // add newNodes (restoring some or all of the old components to the model)
                    syntaxModel.getNodes().addAll(newNodes);

                    // similarly, add the edges
                    List<Edge> newEdges = newComponents.stream().map(Pair::getLeft).collect(Collectors.toList());
                    syntaxModel.getEdges().addAll(newEdges);
                }
            }
        }

        // update entities
        if (CollectionUtilities.hasElements(delta.getEntities())) {
            // if we just want to remove stuff, finish this first
            if (delta.isDeletion()) {
                // now add the new ones with the same uuids but new content
                semanticModel.getNodes().removeAll(delta.getEntities());
            } else {

                List<EntityType> modifiedEntities = new ArrayList<>();
                for (EntityType entity : delta.getEntities()) {
                    // check if entity is valid
                    entity.validate();
                    // get node that is to be overwritten (i.e. uuids match)
                    EntityType replacedNode = semanticModel.getNodes().stream().filter(entityType -> entityType.getUuid().equals(entity.getUuid())).findFirst().orElse(null);

                    if (entity.getSyntaxNodeUuid() != null && (replacedNode == null || replacedNode.getSyntaxNodeUuid() == null)) {
                        // we have to add a mapping
                        PrimitiveEntityTypeMapping explicitMapping = createExplicitMapping(entity);
                        getPrimitiveEntityTypeMappings().add(explicitMapping);
                    } else if (entity.getSyntaxNodeUuid() == null && replacedNode != null && replacedNode.getSyntaxNodeUuid() != null) {
                        // we have to remove a mapping
                        entity.setOriginalLabel(null);
                        getPrimitiveEntityTypeMappings().removeIf(mapping -> mapping.getEntityTypeId().equals(replacedNode.getUuid()));
                    } else if (replacedNode != null && !Objects.equals(entity.getSyntaxNodeUuid(), replacedNode.getSyntaxNodeUuid())) {
                        // there has been a re-assignment use the new one and adjust mapping
                        PrimitiveEntityTypeMapping explicitMapping = createExplicitMapping(entity);
                        getPrimitiveEntityTypeMappings().add(explicitMapping);
                    }
                    modifiedEntities.add(entity);
                    // remove replaced node
                    semanticModel.getNodes().remove(replacedNode);
                }
                semanticModel.getNodes().addAll(modifiedEntities);
            }
        }

        // check that possible changes to the syntax model (e.g. splits) are properly reflected in the entities
        if (!delta.isDeletion() && CollectionUtilities.hasElements(delta.getNodes()) && delta.getNodes().stream().anyMatch(schemaNode -> schemaNode instanceof CompositeNode)) {

            semanticModel.getNodes().stream()
                    .filter(entityType -> entityType.getSyntaxNodeUuid() != null)
                    .forEach(entityType -> {
                        // search for the corresponding syntax node and check if it is still a primary node
                        Optional<SchemaNode> match = delta.getNodes().stream().filter(schemaNode -> schemaNode.getUuid().equals(entityType.getSyntaxNodeUuid())).findFirst();
                        if (match.isPresent() && !(match.get() instanceof PrimitiveNode)) {
                            // not a primitive node any more, remove the link
                            entityType.setSyntaxNodeUuid(null);
                            entityType.setOriginalLabel(null);

                            // also remove the explicit mapping
                            getPrimitiveEntityTypeMappings().removeIf(mapping -> mapping.getPrimitiveUUID().equals(match.get().getUuid()));
                        }
                    });
        }


        // update relations
        if (CollectionUtilities.hasElements(delta.getRelations())) {
            // as we match by uuid only, we can use removeAll and addAll
            // remove all edges that are to be overwritten (i.e. uuids match)
            semanticModel.getEdges().removeAll(delta.getRelations());
            if (!delta.isDeletion()) {
                // now add the new ones with the same uuids but new content
                semanticModel.getEdges().addAll(delta.getRelations());
            }
        }

        // check consistency of semantic model in case elements have been removed
        if (delta.isDeletion()) {
            List<String> deletedTypeIds = delta.getEntities().stream().map(CombinedModelElement::getUuid).collect(Collectors.toList());
            // remove dangling edges
            semanticModel.getEdges().removeIf(relation -> deletedTypeIds.contains(relation.getFromId()) || deletedTypeIds.contains(relation.getToId()));

            // remove mappings that mapped deleted entities to syntax nodes
            getPrimitiveEntityTypeMappings().removeIf(mapping -> deletedTypeIds.contains(mapping.getEntityTypeId()));
        }
    }

    /**
     * Creates an explicit {@link PrimitiveEntityTypeMapping} for the given {@link EntityType} and a syntax node.
     * The syntax node is identified by {@link EntityType#getSyntaxNodeUuid()}.
     * If a mapping already exists for the given entity, this one is deleted first.
     *
     * @param entity The entity to map
     * @return The new mapping
     */
    private PrimitiveEntityTypeMapping createExplicitMapping(EntityType entity) {
        if (syntaxModel == null) {
            throw new UnsupportedOperationException("Syntax Model cannot be null if a modification is applied");
        }

        getPrimitiveEntityTypeMappings().removeIf(mapping -> mapping.getEntityTypeId().equals(entity.getUuid()));
        SchemaNode syntaxNode = syntaxModel.getNodes().stream()
                .filter(schemaNode -> schemaNode.getUuid().equals(entity.getSyntaxNodeUuid()))
                .findFirst().orElseThrow();
        if (!(syntaxNode instanceof PrimitiveNode)) {
            throw new UnsupportedOperationException("Cannot map to syntax node " + syntaxNode.getUuid() + ". Not a primitive node");
        }
        entity.setOriginalLabel(syntaxNode.getLabel());
        // now get parent
        SchemaNode parentNode = syntaxModel.getParentNodeFor(syntaxNode.getUuid());
        // create new explicit mapping
        return new PrimitiveEntityTypeMapping(syntaxNode.getUuid(), entity.getUuid(), parentNode.getUuid());
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity with all components deep cloned
     */
    public CombinedModel copy() {
        return new CombinedModel(
                getDataSourceUuid(),
                getSyntaxModel().copy(),
                getSemanticModel().copy(),
                CollectionUtilities.map(getPrimitiveEntityTypeMappings(),
                        PrimitiveEntityTypeMapping::copy,
                        ArrayList::new),
                CollectionUtilities.map(getRecommendations(),
                        DeltaModification::copy,
                        ArrayList::new),
                isFinalized(),
                CollectionUtilities.map(getProvisionalEntityConcepts(),
                        EntityConcept::copy,
                        ArrayList::new),
                CollectionUtilities.map(getProvisionalRelationConcepts(),
                        RelationConcept::copy,
                        ArrayList::new)
        );
    }

    /**
     * Validates the state of the current model.
     */
    public void validate() {
        List<String> errors = new ArrayList<>();
        // Validate syntax model
        try {
            getSyntaxModel().validate();
        } catch (CombinedModelIntegrityException e) {
            errors.add(e.getMessage());
        }
        // Validate semantic model
        try {
            getSemanticModel().validate();
        } catch (CombinedModelIntegrityException e) {
            errors.add(e.getMessage());
        }

        // Validate links between the models
        getSemanticModel().getNodes().stream()
                .filter(node -> node.getSyntaxNodeUuid() != null)
                .forEach(node -> {
                    if (getSyntaxModel().getNodes().stream().noneMatch(schemaNode -> schemaNode.getUuid().equals(node.getSyntaxNodeUuid()))) {
                        errors.add("EntityType " + node.getUuid() + " references non-existing syntax node " + node.getSyntaxNodeUuid());
                    }
                });
        // Validate recommendations
        validateRecommendations();
        // Validate generated frontend links
        // TODO

        if (!errors.isEmpty()) {
            String errorMessage = String.join("\n", errors);
            throw new CombinedModelIntegrityException(errorMessage);
        }
    }

    /**
     * Validates if the recommendations contained in this model are sound.
     */
    public void validateRecommendations() {
        validateModifications(getRecommendations());
    }

    /**
     * Validates if the given modification is sound.
     *
     * @param modification The {@link DeltaModification} to check
     */
    public void validateModification(DeltaModification modification) {
        validateModifications(Collections.singletonList(modification));
    }

    /**
     * Validates if the given modifications are sound.
     *
     * @param modifications The {@link DeltaModification}s to check
     */
    public void validateModifications(Iterable<DeltaModification> modifications) {
        List<String> errors = new ArrayList<>();

        // uuids of all existing nodes (semantic + syntactic)
        List<String> modelUuids = new ArrayList<>();
        List<String> modelSyntaxUuids = getSyntaxModel().getNodes().stream()
                .map(CombinedModelElement::getUuid)
                .peek(modelUuids::add)
                .collect(Collectors.toList());
        List<String> modelSemanticUuids = getSemanticModel().getNodes().stream()
                .map(CombinedModelElement::getUuid)
                .peek(modelUuids::add)
                .collect(Collectors.toList());

        for (DeltaModification recommendation : modifications) {
            // newly introduced uuids for this modification
            List<String> recommendationSyntaxUuids = recommendation.getNodes().stream()
                    .map(CombinedModelElement::getUuid)
                    .collect(Collectors.toList());
            List<String> recommendationSemanticUuids = recommendation.getEntities().stream()
                    .map(CombinedModelElement::getUuid)
                    .collect(Collectors.toList());

            // Validate the state of this modification
            for (Edge edge : recommendation.getEdges()) {
                if (Stream.concat(modelSyntaxUuids.stream(), recommendationSyntaxUuids.stream())
                        .noneMatch(uuid -> Objects.equals(uuid, edge.getFromId()))) {
                    errors.add("Edge " + edge.getUuid() + " fromId matches " + 0 + " SchemaNodes (must be >= 1)");
                }
                if (Stream.concat(modelSyntaxUuids.stream(), recommendationSyntaxUuids.stream())
                        .noneMatch(uuid -> Objects.equals(uuid, edge.getToId()))) {
                    errors.add("Edge " + edge.getUuid() + " toId matches " + 0 + " SchemaNodes (must be >= 1)");
                }
            }

            for (EntityType entityType : recommendation.getEntities()) {
                EntityConcept entityConcept = entityType.getEntityConcept();
                if (entityConcept == null) {
                    errors.add("EntityType " + entityType.getUuid() + " has no EntityConcept assigned");
                }
            }
            for (Relation edge : recommendation.getRelations()) {
                if (Stream.concat(modelSemanticUuids.stream(), recommendationSemanticUuids.stream())
                        .noneMatch(uuid -> Objects.equals(uuid, edge.getFromId()))) {
                    errors.add("Relation " + edge.getUuid() + " fromId matches " + 0 + " EntityTypes (must be >= 1)");
                }
                if (Stream.concat(modelSemanticUuids.stream(), recommendationSemanticUuids.stream())
                        .noneMatch(uuid -> Objects.equals(uuid, edge.getToId()))) {
                    errors.add("Relation " + edge.getUuid() + " toId matches " + 0 + " EntityTypes (must be >=1 )");
                }
            }
            // Validate links between the models
            recommendation.getEntities().stream()
                    .filter(node -> node.getSyntaxNodeUuid() != null)
                    .forEach(node -> {
                        if (Stream.concat(modelSyntaxUuids.stream(), recommendationSyntaxUuids.stream())
                                .noneMatch(uuid -> uuid.equals(node.getSyntaxNodeUuid()))) {
                            errors.add("EntityType " + node.getUuid() + " references non-existing syntax node " + node.getSyntaxNodeUuid());
                        }
                    });


            // Validate anchors are valid existing syntax / semantic node ids

            recommendation.getAnchors().stream().filter(anchor -> !modelUuids.contains(anchor))
                    .forEach(anchor -> errors.add("No node found for anchor " + anchor + " in recommendation " + recommendation.getReference()));
        }
        if (!errors.isEmpty()) {
            String errorMessage = String.join("\n", errors);
            throw new CombinedModelIntegrityException(errorMessage);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(syntaxModel, semanticModel, recommendations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CombinedModel that = (CombinedModel) o;
        return Objects.equals(syntaxModel, that.syntaxModel) &&
                Objects.equals(semanticModel, that.semanticModel);
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"CombinedModel\""
                + ", \"syntaxModel\":" + syntaxModel
                + ", \"semanticModel\":" + semanticModel
                + ", \"primitiveEntityTypeMappings\":" + StringUtilities.listToJson(primitiveEntityTypeMappings)
                + ", \"recommendations\":" + StringUtilities.listToJson(recommendations)
                + '}';
    }


}
