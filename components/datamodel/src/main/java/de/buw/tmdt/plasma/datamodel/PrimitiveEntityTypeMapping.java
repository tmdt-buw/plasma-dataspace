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

    private static final long serialVersionUID = -6888269571341658964L;

    @NotNull
    private String primitiveUUID;

    @NotNull
    private String instanceId;

    /**
     * The parent syntax node of the node identified by primitiveUUID.
     * Used for rendering.
     */
    @NotNull
    private String parentUUID;

    @JsonCreator
    public PrimitiveEntityTypeMapping(
            @JsonProperty @NotNull String primitiveUUID,
            @JsonProperty @NotNull String instanceId,
            @JsonProperty @NotNull String parentUUID

    ) {
        this.parentUUID = parentUUID;
        this.instanceId = instanceId;
        this.primitiveUUID = primitiveUUID;
    }

    @NotNull
    @JsonProperty
    public String getParentUUID() {
        return parentUUID;
    }

    public void setParentUUID(@NotNull String parentUUID) {
        this.parentUUID = parentUUID;
    }

    @NotNull
    @JsonProperty
    public String getPrimitiveUUID() {
        return primitiveUUID;
    }

    public void setPrimitiveUUID(@NotNull String primitiveUUID) {
        this.primitiveUUID = primitiveUUID;
    }

    @NotNull
    @JsonProperty
    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(@NotNull String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    public PrimitiveEntityTypeMapping copy() {
        return new PrimitiveEntityTypeMapping(getPrimitiveUUID(), getInstanceId(), getParentUUID());
    }

    @Override
    public String toString() {
        return "PrimitiveEntityTypeMapping{" +
                "parentUUID=" + parentUUID +
                ", entityTypeId=" + instanceId +
                ", primitiveUUID=" + primitiveUUID +
                '}';
    }
}
