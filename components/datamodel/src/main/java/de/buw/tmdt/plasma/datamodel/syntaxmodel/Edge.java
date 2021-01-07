package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Edge of the syntax model between two schema nodes.
 */
public class Edge extends CombinedModelElement {

    private static final String FROM_PROPERTY = "from";
    private static final String TO_PROPERTY = "to";
    private static final String KEY_PROPERTY = "key";
    private static final String OPERATIONS_PROPERTY = "operations";
    private static final long serialVersionUID = 40733795713697672L;

    /**
     * An identifier of the origin node, references to a {@link SchemaNode#getUuid()}.
     */
    private String from;

    /**
     * An identifier of the target node, references to a {@link SchemaNode#getUuid()}.
     */
    private String to;

    public Edge(@NotNull String fromUuid, @NotNull String toUuid) {
        this(fromUuid, toUuid, null);
    }

    public Edge(@NotNull String fromUuid, @NotNull String toUuid, String key) {
        this(UUID.randomUUID().toString(), "", fromUuid, toUuid);
    }

    @JsonCreator
    public Edge(
            @JsonProperty(UUID_PROPERTY) @NotNull String uuid,
            @NotNull @JsonProperty(LABEL_PROPERTY) String label,
            @JsonProperty(FROM_PROPERTY) @NotNull String fromUuid,
            @JsonProperty(TO_PROPERTY) @NotNull String toUuid
    ) {
        super(uuid, label);
        this.from = fromUuid;
        this.to = toUuid;
    }

    @JsonProperty(FROM_PROPERTY)
    @NotNull
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @JsonProperty(TO_PROPERTY)
    @NotNull
    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    @Override
    public Edge copy() {
        return new Edge(getUuid(), getLabel(), getFrom(), getTo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Edge edge = (Edge) o;
        return Objects.equals(getUuid(), edge.getUuid());
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"Edge\""
                + ", \"uuid\":" + getUuid()
                + ", \"from\":" + from
                + ", \"to\":" + to
                + '}';
    }
}
