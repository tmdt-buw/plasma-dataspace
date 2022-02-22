package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all elements that can be used inside a combined model.
 */
public abstract class CombinedModelElement implements Serializable {

    public static final String UUID_PROPERTY = "uuid";
    public static final String LABEL_PROPERTY = "label";
    private static final long serialVersionUID = -3619721225687115309L;

    /**
     * Fixed UUID that identifies this object.
     * Available from the creation of the element. Never changes.
     */
    @NotNull
    private String uuid;

    /**
     * The label which is a readable form.
     */
    @NotNull
    private String label;

    public CombinedModelElement(
            @NotNull @JsonProperty(LABEL_PROPERTY) String label
    ) {
        this(UUID.randomUUID().toString(), label);
    }

    @JsonCreator
    public CombinedModelElement(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label
    ) {
        this.uuid = uuid == null ? UUID.randomUUID().toString() : uuid;
        this.label = label;
    }

    @JsonProperty(UUID_PROPERTY)
    @NotNull
    public String getUuid() {
        return uuid;
    }

    @JsonProperty(LABEL_PROPERTY)
    @NotNull
    public String getLabel() {
        return label;
    }

    public void setLabel(@NotNull String label) {
        this.label = label;
    }


    public abstract CombinedModelElement copy();

    @SuppressWarnings("ConstantConditions") // this is only to be invoked before serialization
    public void convertToTemplate() {
        this.uuid = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CombinedModelElement that = (CombinedModelElement) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"CombinedModelElement\""
                + ",\"uuid\":\"" + uuid + '"'
                + ",\"label\":\"" + label + '"'
                + '}';
    }
}
