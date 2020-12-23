package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.buw.tmdt.plasma.datamodel.PositionedCombinedModelElement.X_COORDINATE_PROPERTY;

/**
 * A composite node is existed as a primitive node but was splitted using one or more {@link Splitting} objects.
 */
@JsonTypeName("CompositeDTO")
public class CompositeNode extends SchemaNode {

	private static final long serialVersionUID = 5858068735685107958L;
	private static final String SPLITTER_PROPERTY = "splitter";
	private static final String CLEANSING_PATTERN_PROPERTY = "cleansingPattern";

	private final List<Splitting> splitter;
	private final String cleansingPattern;

	public CompositeNode(
			@NotNull String label,
			@NotNull List<SyntacticOperationDTO> operations,
			 boolean isValid,
			@NotNull  String cleansingPattern,
			@NotNull List<Splitting> splitter
	) {
		super(  label,operations, isValid);
		this.cleansingPattern = cleansingPattern;
		this.splitter = Collections.unmodifiableList(splitter);
	}


	@JsonCreator
	public CompositeNode(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@JsonProperty(LABEL_PROPERTY) @NotNull String label,
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
			@NotNull @JsonProperty(OPERATIONS_PROPERTY) List<SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid,
			@NotNull @JsonProperty(CLEANSING_PATTERN_PROPERTY) String cleansingPattern,
			@NotNull @JsonProperty(SPLITTER_PROPERTY) List<Splitting> splitter
	) {
		super( uuid, label, xCoordinate, yCoordinate,operations, isValid);
		this.cleansingPattern = cleansingPattern;
		this.splitter = Collections.unmodifiableList(splitter);
	}

	@JsonProperty(CLEANSING_PATTERN_PROPERTY)
	@NotNull
	public String getCleansingPattern() {
		return cleansingPattern;
	}

	@NotNull
	@JsonProperty(SPLITTER_PROPERTY)
	public List<Splitting> getSplitter() {
		return Collections.unmodifiableList(splitter);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"CompositeDTO\""
		       + ", \"@super\":" + super.toString()
		       + ", \"cleansingPattern\":\"" + cleansingPattern + '"'
		       + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		CompositeNode that = (CompositeNode) o;
		return Objects.equals(cleansingPattern, that.cleansingPattern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), cleansingPattern);
	}
}
