package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Base class for all elements that can be used inside a combined model
 */
public class CombinedModelElement implements Serializable {

	public static final String UUID_PROPERTY = "uuid";
	public static final String DATABASE_ID_PROPERTY = "databaseId";
	public static final String LABEL_PROPERTY = "label";
	private static final long serialVersionUID = -3619721225687115309L;

	/**
	 * Fixed UUID that identifies this object.
	 * Available from the creation of the element. Never changes.
	 */
	private final String uuid;
	/**
	 * Database ID, will be changed after each persistence of the model
	 */
	private final Long databaseId;
	/**
	 * The label which is a readable form
	 */
	private final String label;

	public CombinedModelElement(
			@NotNull @JsonProperty(LABEL_PROPERTY) String label
	) {
		this(UUID.randomUUID().toString(),null,label);
	}

	@JsonCreator
	public CombinedModelElement(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label
	) {
		this.uuid = uuid;
		this.databaseId = databaseId;
		this.label = label;
	}

	@JsonProperty(UUID_PROPERTY)
	@NotNull
	public String getUuid() {
		return uuid;
	}

	@JsonProperty(DATABASE_ID_PROPERTY)
	@Nullable
	public String getSerializedDatabaseId() {
		return StringUtilities.toStringIfExists(databaseId);
	}
	@JsonIgnore
	@Nullable
	public Long getDatabaseId() {
		return databaseId;
	}

	@JsonProperty(LABEL_PROPERTY)
	@NotNull
	public String getLabel() {
		return label;
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"Positioned\""
		+ ",\"uuid\":\"" + uuid+ '"'
				+ ",\"databaseId\":\"" + databaseId + '"'
				+ ",\"label\":\"" + label + '"'
		       + '}';
	}
}
