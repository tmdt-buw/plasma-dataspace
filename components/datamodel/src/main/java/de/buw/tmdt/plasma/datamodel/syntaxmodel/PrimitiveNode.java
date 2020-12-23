package de.buw.tmdt.plasma.datamodel.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import de.buw.tmdt.plasma.datamodel.modification.operation.SyntacticOperationDTO;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@JsonTypeName("PrimitiveDTO")
public class PrimitiveNode extends SchemaNode {

	private static final long serialVersionUID = -144653268470507986L;

	private static final String CLEANSING_PATTERN_PROPERTY = "cleansingPattern";
	private static final String EXAMPLES_PROPERTY = "examples";
	private static final String DATA_TYPE_PROPERTY = "dataType";

	private final String cleansingPattern;
	private final ArrayList<String> examples;
	private final String dataType;

	@JsonCreator
	public PrimitiveNode(
			@JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate,
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@JsonProperty(LABEL_PROPERTY) @NotNull String label,
			@NotNull @JsonProperty(OPERATIONS_PROPERTY) List<@NotNull SyntacticOperationDTO> operations,
			@JsonProperty(IS_VALID_PROPERTY) boolean isValid,
			@NotNull @JsonProperty(DATA_TYPE_PROPERTY) String dataType,
			@JsonProperty(EXAMPLES_PROPERTY) List<String> examples,
			@JsonProperty(CLEANSING_PATTERN_PROPERTY) String cleansingPattern
	) {
		super( uuid, label, xCoordinate, yCoordinate,operations, isValid);
		this.dataType = dataType;
		this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
		this.cleansingPattern = cleansingPattern;
	}

	@JsonProperty(CLEANSING_PATTERN_PROPERTY)
	public String getCleansingPattern() {
		return cleansingPattern;
	}

	@JsonProperty(EXAMPLES_PROPERTY)
	public List<String> getExamples() {
		return Collections.unmodifiableList(examples);
	}

	@JsonProperty(DATA_TYPE_PROPERTY)
	public String getDataType() {
		return dataType;
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"PrimitiveDTO\""
		       + ", \"@super\":" + super.toString()
		       + ", \"cleansingPattern\":\"" + cleansingPattern + '"'
		       + ", \"examples\":" + StringUtilities.listToJson(examples)
		       + ", \"dataType\":\"" + dataType + '"'
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
		PrimitiveNode that = (PrimitiveNode) o;
		return 	       Objects.equals(cleansingPattern, that.cleansingPattern) &&
		       Objects.equals(examples, that.examples) &&
		       Objects.equals(dataType, that.dataType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), cleansingPattern, dataType);
	}
}
