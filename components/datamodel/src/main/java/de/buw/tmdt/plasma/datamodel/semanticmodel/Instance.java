package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * An instantiation resembles the use of a concept inside a semantic model in a combined model.
 */
public class Instance extends CombinedModelElement implements Serializable {

    private static final long serialVersionUID = 4109691502825894813L;

    public static final String SYNTAX_NODE_PROPERTY = "syntaxNodeUuid";
    public static final String ORIGINAL_LABEL_PROPERTY = "originalLabel";
    public static final String URI_PROPERTY = "uri";
    public static final String DESCRIPTION_PROPERTY = "description";

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

    private String uri;

    /**
     * Constructor to use for a new instantiation.
     *
     * @param label       The label to show
     * @param description A description for this object, optional
     */
    public Instance(@NotNull String label,
                    @Nullable String description) {
        super(null, label);
        this.description = description;
    }


    /**
     * Constructor to use for a new mapped instantiation.
     *
     * @param label          The label to show
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     */
    public Instance(@NotNull String label,
                    @NotNull String syntaxNodeUuid,
                    @NotNull String originalLabel,
                    @Nullable String description) {
        super(null, label);
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
    }

    /**
     * Constructor to use for a new mapped instantiation at a specific place.
     *
     * @param uuid           The object uuid
     * @param uri            The instance URI, only set if this has been submitted to the knowledge base
     * @param label          The label to show
     * @param syntaxNodeUuid The syntax node id to be mapped to.
     *                       Should be equal to a {@link PrimitiveNode#getUuid()}
     * @param originalLabel  The original attributes label
     * @param description    A description for this object, optional
     */
    @JsonCreator
    public Instance(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @Nullable @JsonProperty(URI_PROPERTY) String uri,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(SYNTAX_NODE_PROPERTY) String syntaxNodeUuid,
            @Nullable @JsonProperty(ORIGINAL_LABEL_PROPERTY) String originalLabel,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description
    ) {
        super(uuid, label);
        this.uri = uri;
        this.syntaxNodeUuid = syntaxNodeUuid;
        this.originalLabel = originalLabel;
        this.description = description;
    }

    /* Getter */
    @JsonProperty(SYNTAX_NODE_PROPERTY)
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

    @JsonProperty(URI_PROPERTY)
    public String getURI() {
        return uri;
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

    public void setURI(String uri) {
        this.uri = uri;
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
                getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getURI(), originalLabel, description);
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
        return Objects.equals(this.getLabel(), that.getLabel()) &&
                Objects.equals(this.getDescription(), that.getDescription()) &&
                Objects.equals(this.getOriginalLabel(), that.getOriginalLabel()) &&
                Objects.equals(this.getSyntaxNodeUuid(), that.getSyntaxNodeUuid());
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
    }

}