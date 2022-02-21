package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@JsonTypeName("DataProperty")
public class DataProperty extends Relation {
    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String sourceURI) {
        super(fromId, toId, sourceURI);
    }

    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String sourceURI, @NotNull String label) {
        super(fromId, toId, sourceURI, label);
    }

    public DataProperty(@NotNull String fromId, @NotNull String toId, @NotNull String sourceURI, @NotNull String label, @NotNull String description) {
        super(fromId, toId, sourceURI, label, description);
    }

    public DataProperty(@Nullable String uuid, @NotNull String fromId, @NotNull String toId, @NotNull String sourceURI, @Nullable String label, @Nullable String description, @Nullable Set<String> properties) {
        super(uuid, fromId, toId, sourceURI, label, description, properties);
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
                getSourceURI(),
                getLabel(),
                getDescription(),
                getProperties()
        );
    }
}
