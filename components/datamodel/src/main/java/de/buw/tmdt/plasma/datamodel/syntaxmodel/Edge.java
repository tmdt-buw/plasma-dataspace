package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Edge of the syntax model between two schema nodes.
 */
public class Edge extends CombinedModelElement {

    private static final String FROM_PROPERTY = "from";
    private static final String TO_PROPERTY = "to";
    private static final long serialVersionUID = 40733795713697672L;
    private static final String ANNOTATION_PROPERTY = "annotation";

    /**
     * An identifier of the origin node, references to a {@link SchemaNode#getUuid()}.
     */
    @NotNull
    private String from;

    /**
     * An identifier of the target node, references to a {@link SchemaNode#getUuid()}.
     */
    @NotNull
    private String to;

    /**
     * Optional additional annotation / information for this edge.
     */
    @Nullable
    private String annotation;

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION") // does not intent to override
    public Edge(@NotNull String fromUuid, @NotNull String toUuid) {
        this(UUID.randomUUID().toString(), "", fromUuid, toUuid, null);
    }

    public Edge(
            @JsonProperty(UUID_PROPERTY) @NotNull String uuid,
            @JsonProperty(LABEL_PROPERTY) @NotNull String label,
            @JsonProperty(FROM_PROPERTY) @NotNull String fromUuid,
            @JsonProperty(TO_PROPERTY) @NotNull String toUuid
    ) {
        this(uuid, label, fromUuid, toUuid, null);
    }

    @JsonCreator
    public Edge(
            @JsonProperty(UUID_PROPERTY) @NotNull String uuid,
            @JsonProperty(LABEL_PROPERTY) @NotNull String label,
            @JsonProperty(FROM_PROPERTY) @NotNull String fromUuid,
            @JsonProperty(TO_PROPERTY) @NotNull String toUuid,
            @JsonProperty(ANNOTATION_PROPERTY) @Nullable String annotation
    ) {
        super(uuid, label);
        this.from = fromUuid;
        this.to = toUuid;
        this.annotation = annotation;
    }

    @JsonProperty(FROM_PROPERTY)
    @NotNull
    public String getFrom() {
        return from;
    }

    public void setFrom(@NotNull String from) {
        this.from = from;
    }

    @JsonProperty(TO_PROPERTY)
    @NotNull
    public String getTo() {
        return to;
    }

    public void setTo(@NotNull String to) {
        this.to = to;
    }

    @Nullable
    @JsonProperty(ANNOTATION_PROPERTY)
    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(@Nullable String annotation) {
        this.annotation = annotation;
    }

    /**
     * Creates a deep clone of the current entity.
     *
     * @return The cloned entity
     */
    @Override
    public Edge copy() {
        return new Edge(getUuid(), getLabel(), getFrom(), getTo(), getAnnotation());
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
