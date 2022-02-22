package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * An instantiation resembles the use of a concept inside a semantic model in a combined model.
 */
public class Instance extends SemanticModelNode implements Serializable {

    private static final long serialVersionUID = 4109691502825894813L;

    /**
     * The id of the syntax {@link PrimitiveNode} this object is mapped to.
     * Will be null if not mapped.
     */
    private String syntaxNodeUuid;

    /**
     * The original label of an attribute if this instantiation is mapped to one, null otherwise.
     */
    private String originalLabel;

    private String description;

    /**
     * The assigned {@link SemanticClass} that defines this {@link Instance}.
     */
    private SemanticClass semanticClass;

    /**
     * Constructor to use for a new unmapped instantiation.
     *
     * @param label         The label to show
     * @param description   A description for this object, optional
     * @param SemanticClass The concept to represent
     */
    public Instance(@NotNull String label,
                    String description,
                    SemanticClass SemanticClass) {
        super(null, label);
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /**
     * Constructor to use for a new unmapped instantiation.
     * This constructor also accepts a URI if there exists a representation of this element somewhere.
     *
     * @param label         The label to show
     * @param uri           An uri to represent this instance
     * @param description   A description for this object, optional
     * @param SemanticClass The concept to represent
     */
    public Instance(@NotNull String uri,
                    @NotNull String label,
                    String description,
                    SemanticClass SemanticClass) {
        super(uri, label);
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /**
     * Constructor to use for a new unmapped instantiation at a specific place.
     *
     * @param label         The label to show
     * @param xCoordinate   X coordinate
     * @param yCoordinate   Y coordinate
     * @param description   A description for this object, optional
     * @param SemanticClass The concept to represent
     */
    public Instance(@NotNull String label,
                    @NotNull Double xCoordinate,
                    @NotNull Double yCoordinate,
                    @Nullable String description,
                    @NotNull SemanticClass SemanticClass) {
        super(null, label, xCoordinate, yCoordinate);
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /**
     * Constructor to use for a new unmapped instantiation at a specific place.
     *
     * @param uri           An existing entity id of this element
     * @param label         The label to show
     * @param xCoordinate   X coordinate
     * @param yCoordinate   Y coordinate
     * @param description   A description for this object, optional
     * @param SemanticClass The concept to represent
     */
    public Instance(@NotNull String uri,
                    @NotNull String label,

                    @NotNull Double xCoordinate,
                    @NotNull Double yCoordinate,
                    @Nullable String description,
                    @NotNull SemanticClass SemanticClass) {
        super(uri, label, xCoordinate, yCoordinate);
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /**
     * Constructor to use for a new mapped instantiation at a specific place.
     *
     * @param label          The label to show
     * @param xCoordinate    X coordinate
     * @param yCoordinate    Y coordinate
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     * @param SemanticClass  The concept to represent
     */
    public Instance(@NotNull String label,
                    @Nullable Double xCoordinate,
                    @Nullable Double yCoordinate,
                    @NotNull String syntaxNodeUuid,
                    @NotNull String originalLabel,
                    @Nullable String description,
                    @NotNull SemanticClass SemanticClass) {
        super(null, label, xCoordinate, yCoordinate);
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /**
     * Constructor to use for a new mapped instantiation at a specific place.
     *
     * @param uuid           The object uuid
     * @param uri            The instance URI, only set if this has been submitted to the knowledge base
     * @param label          The label to show
     * @param xCoordinate    X coordinate
     * @param yCoordinate    Y coordinate
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     * @param SemanticClass  The concept to represent
     */
    @JsonCreator
    public Instance(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @Nullable @JsonProperty String uri,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty String syntaxNodeUuid,
            @Nullable @JsonProperty String originalLabel,
            @Nullable @JsonProperty String description,
            @NotNull @JsonProperty SemanticClass SemanticClass,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate
    ) {
        super(uuid, uri, label, xCoordinate, yCoordinate);
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
        this.semanticClass = SemanticClass;
    }

    /* Getter */
    @JsonProperty
    public String getSyntaxNodeUuid() {
        return syntaxNodeUuid;
    }

    @JsonProperty
    public String getOriginalLabel() {
        return originalLabel;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public SemanticClass getSemanticClass() {
        return semanticClass;
    }

    /* Setter */

    public void setSyntaxNodeUuid(String syntaxNodeUuid) {
        this.syntaxNodeUuid = syntaxNodeUuid;
    }

    public void setOriginalLabel(String originalLabel) {
        this.originalLabel = originalLabel;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSemanticClass(SemanticClass semanticClass) {
        this.semanticClass = semanticClass;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity with also the EntityConcept cloned
     */
    @Override
    public Instance copy() {
        return new Instance(getUuid(),
                getURI(),
                getLabel(),
                getSyntaxNodeUuid(),
                getOriginalLabel(),
                getDescription(),
                getSemanticClass().copy(),
                getXCoordinate(),
                getYCoordinate());
    }

    @Override
    public Instance build() {
        return new Instance(null,
                getURI(),
                getLabel(),
                getSyntaxNodeUuid(),
                getOriginalLabel(),
                getDescription(),
                getSemanticClass().copy(),
                getXCoordinate(),
                getYCoordinate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), originalLabel, description, semanticClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Instance that = (Instance) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"Instance\""
                + ", \"uuid\":\"" + getUuid() + '"'
                + ", \"URI\":\"" + getURI() + '"'
                + ", \"label\":\"" + getLabel() + '"'
                + ", \"syntaxNodeUuid\":\"" + syntaxNodeUuid + '"'
                + ", \"originalLabel\":\"" + originalLabel + '"'
                + ", \"description\":\"" + description + '"'
                + '}';
    }

    public void validate() throws CombinedModelIntegrityException {
        //if (getLabel().length() < 3) {
        //throw new CombinedModelIntegrityException("Label of EntityType '" + getUuid() + "' must not be shorter than 3 characters.");
        //}
        if (getSemanticClass() == null) {
            throw new CombinedModelIntegrityException("Instance " + getUuid() + " has no SemanticConcept assigned");
        }
        getSemanticClass().validate();
    }
}