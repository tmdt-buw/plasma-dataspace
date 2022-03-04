package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@JsonTypeName("ObjectProperty")
public class ObjectProperty extends Relation {
    public ObjectProperty( String fromId,  String toId, @NotNull String uri) {
        super(fromId, toId, uri);
    }

    public ObjectProperty( String fromId,  String toId, @NotNull String uri, @NotNull String label) {
        super(fromId, toId, uri, label);
    }

    public ObjectProperty(String fromId, String toId, @NotNull String uri, @NotNull String label, @NotNull String description) {
        super(fromId, toId, uri, label, description);
    }

    @JsonCreator
    public ObjectProperty(
            @Nullable @JsonProperty(UUID_PROPERTY) String uuid,
            @Nullable @JsonProperty(FROM_PROPERTY) String from,
            @Nullable @JsonProperty(TO_PROPERTY) String to,
            @NotNull @JsonProperty(URI_PROPERTY) String uri,
            @Nullable @JsonProperty(LABEL_PROPERTY) String label,
            @Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
            @Nullable @JsonProperty(PROPERTIES_PROPERTY) Set<String> properties
    ) {
        super(uuid,from,to,uri,label,description,properties);
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"ObjectProperty\""
                + ", \"fromId\":\"" + getFrom() + '"'
                + ", \"toId\":\"" + getTo() + '"'
                + ", \"URI\":" + getURI()
                + '}';
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    public ObjectProperty copy() {
        return new ObjectProperty(getUuid(),
                getFrom(),
                getTo(),
                getURI(),
                getLabel(),
                getDescription(),
                getProperties()
        );
    }

    @Override
    public ObjectProperty instanciate(String fromId, String toId) {
        return new ObjectProperty(fromId, toId, getURI(), getLabel(), getDescription());
    }

}
