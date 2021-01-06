package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.collections.CollectionUtilities;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Edge of the syntax model between two schema nodes
 */
public class Edge extends CombinedModelElement implements Cloneable {

	private static final String FROM_PROPERTY = "from";
	private static final String TO_PROPERTY = "to";
	private static final String KEY_PROPERTY = "key";
	private static final String OPERATIONS_PROPERTY = "operations";
	private static final long serialVersionUID = 40733795713697672L;

	/**
	 * An identifier of the origin node, references to a {@link SchemaNode#getUuid()}
	 */
	private String from;
	/**
	 * An identifier of the target node, references to a {@link SchemaNode#getUuid()}
	 */
	private String to;
	/**
	 * ?
	 */
	private String key;
	/**
	 * List of possible operations performable on this edge.
	 */
	private final ArrayList<SyntacticOperationDTO> operations;

	public Edge(@NotNull String fromUuid, @NotNull String toUuid) {
		this(fromUuid, toUuid, null);
	}

	public Edge(@NotNull String fromUuid, @NotNull String toUuid, String key) {
		this(UUID.randomUUID().toString(),"",fromUuid, toUuid, key, null);
	}

	@JsonCreator
	public Edge(
			@JsonProperty(UUID_PROPERTY) @NotNull String uuid,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@JsonProperty(FROM_PROPERTY) @NotNull String fromUuid,
			@JsonProperty(TO_PROPERTY) @NotNull String toUuid,
			@JsonProperty(KEY_PROPERTY) @Nullable String key,
			@JsonProperty(OPERATIONS_PROPERTY) @Nullable List<SyntacticOperationDTO> operations
	) {
		super(uuid,label);
			this.from =fromUuid;
			this.to = toUuid;
		this.key = key;
		this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
	}

	@JsonProperty(FROM_PROPERTY)
	@NotNull
	public String getFrom() {
		return from;
	}

	@JsonProperty(TO_PROPERTY)
	@NotNull
	public String getTo() {
		return to;
	}

	@JsonProperty(KEY_PROPERTY)
	@Nullable
	public String getKey() {
		return key;
	}

	@JsonProperty(OPERATIONS_PROPERTY)
	@Nullable
	public List<SyntacticOperationDTO> getOperations() {
		return operations;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Creates a deep clone of the current entity
	 *
	 * @return The cloned entity
	 */
	@Override
	public Edge clone() {
		return new Edge(getUuid(), getLabel(), getFrom(), getTo(), getKey(),
				CollectionUtilities.map(getOperations(),
						SyntacticOperationDTO::clone,
						ArrayList::new));
	}

	@Override
	public int hashCode() {

		return Objects.hash(from, to, key, operations);
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
				+ ", \"from\":" + from
				+ ", \"to\":" + to
				+ ", \"key\":\"" + key + '"'
				+ ", \"operations\":" + StringUtilities.listToJson(operations)
				+ '}';
	}
}
