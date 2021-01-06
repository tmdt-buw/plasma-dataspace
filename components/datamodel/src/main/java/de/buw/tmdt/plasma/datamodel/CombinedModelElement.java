package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all elements that can be used inside a combined model
 */
public class CombinedModelElement implements Serializable, Cloneable {

    public static final String UUID_PROPERTY = "uuid";
    public static final String LABEL_PROPERTY = "label";
    private static final long serialVersionUID = -3619721225687115309L;

    /**
     * Fixed UUID that identifies this object.
     * Available from the creation of the element. Never changes.
     */
    private final String uuid;
	/**
	 * The label which is a readable form
	 */
	private String label;

	public CombinedModelElement(
			@NotNull @JsonProperty(LABEL_PROPERTY) String label
	) {
		this(UUID.randomUUID().toString(),label);
	}

	@JsonCreator
	public CombinedModelElement(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label
	) {
		this.uuid = uuid;
		this.label = label;
	}

	@JsonProperty(UUID_PROPERTY)
	@NotNull
	public String getUuid() {
		return uuid;
	}

	@JsonProperty(LABEL_PROPERTY)
	@NotNull
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public CombinedModelElement clone() {
		return new CombinedModelElement(getUuid(), getLabel());
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CombinedModelElement that = (CombinedModelElement) o;
		return Objects.equals(getUuid(), that.getUuid());
	}

	@Override
	public int hashCode() {
		return getUuid().hashCode();
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"Positioned\""
		+ ",\"uuid\":\"" + uuid+ '"'
				+ ",\"label\":\"" + label + '"'
		       + '}';
	}
}
