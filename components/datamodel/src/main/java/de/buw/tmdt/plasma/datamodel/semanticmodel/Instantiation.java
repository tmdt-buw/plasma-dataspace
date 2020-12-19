package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import static de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement.X_COORDINATE_PROPERTY;

/**
 * An instantiation resembles the use of a concept inside a semantic model in a combined model.
 * After modeling, will be converted into an Entity Concept
 */
public class Instantiation extends PositionedCombinedModelElement implements Serializable {

    private static final long serialVersionUID = 4109691502825894813L;

    public static final String ENTITY_ID_PROPERTY = "entityId";
    public static final String SYNTAX_NODE_ID_PROPERTY = "syntaxNodeId";
    public static final String ORIGINAL_LABEL_PROPERTY = "originalLabel";
    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String CONCEPT_PROPERTY = "concept";
    public static final String MAPPED_TO_DATA_PROPERTY = "mappedToData";

    /**
     * Knowledge base ID, set once sent to the KGS
     */
    private String entityId;
    /**
     * The id of the syntax {@link de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode}  this object is mapped to.
     * Will be null if not mapped
     */
    private String syntaxNodeUuid;
    /**
     * The original label of an attribute if this instantiation is mapped to one, null otherwise
     */
    private String originalLabel;

    private String description;
    private final Concept concept;

    /**
     * Constructor to use for a new unmapped instantiation
     *
     * @param label       The label to show
     * @param description A description for this object, optional
     * @param concept     The concept to represent
     */
    public Instantiation(@NotNull String label,
                         String description,
                         Concept concept) {
        super(label);
        this.description = description;
        this.concept = concept;
    }

    /**
     * Constructor to use for a new unmapped instantiation at a specific place
     *
     * @param label       The label to show
     * @param xCoordinate X coordinate
     * @param yCoordinate Y coordinate
     * @param description A description for this object, optional
     * @param concept     The concept to represent
     */
    public Instantiation(@NotNull String label,
                         @NotNull Double xCoordinate,
                         @NotNull Double yCoordinate,
                         @Nullable String description,
                         @NotNull Concept concept) {
        super(label, xCoordinate, yCoordinate);
        this.description = description;
        this.concept = concept;
    }

    /**
     * Constructor to use for a new mapped instantiation at a specific place
     *
     * @param label          The label to show
     * @param xCoordinate    X coordinate
     * @param yCoordinate    Y coordinate
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     * @param concept        The concept to represent
     */
    public Instantiation(@NotNull String label,
                         @Nullable Double xCoordinate,
                         @Nullable Double yCoordinate,
                         @NotNull String syntaxNodeUuid,
                         @NotNull String originalLabel,
                         @Nullable String description,
                         @NotNull Concept concept) {
        super(label, xCoordinate, yCoordinate);
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
        this.concept = concept;
    }

    /**
     * Constructor to use for a new mapped instantiation at a specific place
     *
     * @param uuid           The object uuid
     * @param databaseId     The database id if it has already been persisted
     * @param entityId       The entity id, only set if this has beem submitted to the knowledge base
     * @param label          The label to show
     * @param xCoordinate    X coordinate
     * @param yCoordinate    Y coordinate
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     * @param concept        The concept to represent
     */
    @JsonCreator
    public Instantiation(
            @NotNull @JsonProperty(UUID_PROPERTY) String uuid,
            @Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
            @Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @NotNull @JsonProperty(SYNTAX_NODE_ID_PROPERTY) String syntaxNodeUuid,
            @NotNull @JsonProperty(ORIGINAL_LABEL_PROPERTY) String originalLabel,
            @NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @NotNull @JsonProperty(CONCEPT_PROPERTY) Concept concept,
            @Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
            @Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate
    ) {
        super(uuid, databaseId, label, xCoordinate, yCoordinate);
        this.entityId = entityId;
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
        this.concept = concept;
    }

    /* Getter */
    @JsonProperty(ENTITY_ID_PROPERTY)
    public String getEntityId() {
        return entityId;
    }

    @JsonProperty(SYNTAX_NODE_ID_PROPERTY)
    public String getSyntaxNodeUuid() {
        return syntaxNodeUuid;
    }

    @JsonProperty(ORIGINAL_LABEL_PROPERTY)
    public String getOriginalLabel() {
        return originalLabel;
    }

    @JsonProperty(DESCRIPTION_PROPERTY)
    public String getDescription() {
        return description;
    }

    @JsonProperty(CONCEPT_PROPERTY)
    public Concept getConcept() {
        return concept;
    }

    @JsonProperty(MAPPED_TO_DATA_PROPERTY)
    public Boolean isMappedToData() {
        return originalLabel != null;
    }

    /* Setter */

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setSyntaxNodeUuid(String syntaxNodeUuid) {
        this.syntaxNodeUuid = syntaxNodeUuid;
    }

    public void setOriginalLabel(String originalLabel) {
        this.originalLabel = originalLabel;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), originalLabel, description, concept);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Instantiation that = (Instantiation) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"EntityConceptDTO\""
                + ", \"uuid\":\"" + getUuid() + '"'
                + ", \"databaseId\":\"" + getDatabaseId() + '"'
                + ", \"entityId\":\"" + entityId + '"'
                + ", \"label\":\"" + getLabel() + '"'
                + ", \"syntaxNodeUuid\":\"" + syntaxNodeUuid + '"'
                + ", \"originalLabel\":\"" + originalLabel + '"'
                + ", \"description\":\"" + description + '"'
                + '}';
    }

}