package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Base class for all elements that can be used inside a combined model that have an explicit placement
 */
public abstract class PositionedCombinedModelElement extends CombinedModelElement implements Serializable {

	public static final String X_COORDINATE_PROPERTY = "x";
	public static final String Y_COORDINATE_PROPERTY = "y";
	private static final long serialVersionUID = -3619721225687115309L;

	private final Double xCoordinate;
	private final Double yCoordinate;

	public PositionedCombinedModelElement(
			@NotNull @JsonProperty(LABEL_PROPERTY) String label
	) {
		super(label);
		this.xCoordinate = null;
		this.yCoordinate = null;
	}

	public PositionedCombinedModelElement(
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate
	) {
		super(label);
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
	}

	@JsonCreator
	public PositionedCombinedModelElement(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@Nullable @JsonProperty(X_COORDINATE_PROPERTY) Double xCoordinate,
			@Nullable @JsonProperty(Y_COORDINATE_PROPERTY) Double yCoordinate
	) {
		super(uuid,label);
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
	}

	@Nullable
	@JsonProperty(X_COORDINATE_PROPERTY)
	public Double getXCoordinate() {
		return xCoordinate;
	}

	@Nullable
	@JsonProperty(Y_COORDINATE_PROPERTY)
	public Double getYCoordinate() {
		return yCoordinate;
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"Positioned\""
		+    ",\"parent\":\"" + super.toString()+ '"'
		       + ",\"xCoordinate\":\"" + xCoordinate + '"'
		       + ",\"yCoordinate\":\"" + yCoordinate + '"'
		       + '}';
	}
}
