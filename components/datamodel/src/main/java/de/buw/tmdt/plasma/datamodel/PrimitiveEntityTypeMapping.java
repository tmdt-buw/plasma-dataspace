package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Contains mappings needed to render supplementary edges in the frontend.
 * Only used for frontend issues, can be ignored everywhere else
 */
public class PrimitiveEntityTypeMapping implements Serializable {

    private static final String PARENT_UUID = "from";
    private static final String ENTITY_TYPE_ID = "to";
    private static final String PRIMITIVE_UUID = "mappedPrimitive";
    private static final long serialVersionUID = -6888269571341658964L;

    @NotNull
    private String primitiveUUID;

    @NotNull
    private String entityTypeId;

    /**
     * The parent syntax node of the node identified by primitiveUUID.
     * Used for rendering.
     */
    @NotNull
    private String parentUUID;

    @JsonCreator
    public PrimitiveEntityTypeMapping(
            @JsonProperty(PRIMITIVE_UUID) @NotNull String primitiveUUID,
            @JsonProperty(ENTITY_TYPE_ID) @NotNull String entityTypeId,
            @JsonProperty(PARENT_UUID) @NotNull String parentUUID

    ) {
        this.parentUUID = parentUUID;
        this.entityTypeId = entityTypeId;
        this.primitiveUUID = primitiveUUID;
    }

    @NotNull
    @JsonProperty(PARENT_UUID)
    public String getParentUUID() {
        return parentUUID;
    }

    public void setParentUUID(@NotNull String parentUUID) {
        this.parentUUID = parentUUID;
    }

    @NotNull
    @JsonProperty(PRIMITIVE_UUID)
    public String getPrimitiveUUID() {
        return primitiveUUID;
    }

    public void setPrimitiveUUID(@NotNull String primitiveUUID) {
        this.primitiveUUID = primitiveUUID;
    }

    @NotNull
    @JsonProperty(ENTITY_TYPE_ID)
    public String getEntityTypeId() {
        return this.entityTypeId;
    }

    public void setEntityTypeId(@NotNull String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    public PrimitiveEntityTypeMapping copy() {
        return new PrimitiveEntityTypeMapping(getPrimitiveUUID(), getEntityTypeId(), getParentUUID());
    }

    @Override
    public String toString() {
        return "PrimitiveEntityTypeMapping{" +
                "parentUUID=" + parentUUID +
                ", entityTypeId=" + entityTypeId +
                ", primitiveUUID=" + primitiveUUID +
                '}';
    }
}
