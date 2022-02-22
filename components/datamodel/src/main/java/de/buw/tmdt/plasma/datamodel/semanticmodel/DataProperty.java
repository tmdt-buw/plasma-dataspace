package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@JsonTypeName("DataProperty")
public class DataProperty extends Relation {
    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String uri) {
        super(fromId, toId, uri);
    }

    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String uri, @NotNull String label) {
        super(fromId, toId, uri, label);
    }

    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String uri, @NotNull String label, @NotNull String description) {
        super(fromId, toId, uri, label, description);
    }

    public DataProperty(@Nullable String uuid, @NotNull String fromId, @NotNull String toId, @NotNull String uri, @Nullable String label, @Nullable String description, @Nullable Set<String> properties) {
        super(uuid, fromId, toId, uri, label, description, properties);
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"DataProperty\""
                + ", \"fromId\":\"" + getFromId() + '"'
                + ", \"toId\":\"" + getToId() + '"'
                + ", \"URI\":" + getURI()
                + '}';
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    public DataProperty copy() {
        return new DataProperty(getUuid(),
                getFromId(),
                getToId(),
                getURI(),
                getLabel(),
                getDescription(),
                getProperties()
        );
    }

    @Override
    public DataProperty build(String fromId, String toId) {
        return new DataProperty(fromId, toId, getURI(), getLabel(), getDescription());
    }
}
