package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RelationConcept extends CombinedModelElement implements Serializable {
	private static final long serialVersionUID = -5112996969222323409L;

	private static final String ENTITY_ID_PROPERTY = "entityId";
	private static final String DESCRIPTION_PROPERTY = "description";
	private static final String SOURCE_URI_PROPERTY = "sourceURI";
	private static final String PROPERTIES_PROPERTY = "properties";

	/**
	 * Entity ID, set once persisted in the KGS.
	 */
	private String entityId;
	private final String description;
	private final String sourceURI;
	private final Set<String> properties;

	public RelationConcept(
			@NotNull String label,
			@Nullable String description,
			@Nullable String entityId,
			@NotNull String sourceURI,
			@NotNull Set<String> properties
	) {
		super(label);
		this.entityId = entityId;
		this.description = description;
		this.sourceURI = sourceURI;
		this.properties = new HashSet<>(properties);
	}

	@JsonCreator
	public RelationConcept(
			@Nullable @JsonProperty(UUID_PROPERTY) String uuid,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@Nullable @JsonProperty(ENTITY_ID_PROPERTY) String entityId,
			@Nullable @JsonProperty(DESCRIPTION_PROPERTY) String description,
			@NotNull @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI,
			@NotNull @JsonProperty(PROPERTIES_PROPERTY) Set<String> properties
	) {
		super(uuid, label);
		this.entityId = entityId;
		this.description = description;
		this.sourceURI = sourceURI;
		this.properties = properties;
	}

	@JsonProperty(ENTITY_ID_PROPERTY)
	public String getEntityId() {
		return entityId;
	}

	@JsonProperty(DESCRIPTION_PROPERTY)
	public String getDescription() {
		return description;
	}

	@JsonProperty(SOURCE_URI_PROPERTY)
	public String getSourceURI() {
		return sourceURI;
	}

	@JsonProperty(PROPERTIES_PROPERTY)
	public Set<String> getProperties() {
		return Collections.unmodifiableSet(this.properties);
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public int hashCode() {
		return this.getUuid().hashCode();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass().equals(o.getClass())) {
			RelationConcept that = (RelationConcept) o;
			return Objects.equals(getUuid(), that.getUuid());
		} else {
			return false;
		}
	}

	/**
	 * Creates a deep clone of the current entity.
	 *
	 * @return The cloned entity with also the RelationConcept cloned
	 */
	public RelationConcept copy() {
		return new RelationConcept(
				getUuid(),
				getLabel(),
				getEntityId(),
				getDescription(),
				getSourceURI(),
				getProperties()
		);
	}

	/**
	 * Identifies if two {@link RelationConcept}s represent the same concept.
	 * Needed as there might exist multiple {@link RelationConcept} instances that actually represent the same concept
	 * but have different uuids.
	 *
	 * @param relationConcept The {@link RelationConcept} to check
	 * @return true of the {@link #sourceURI}s of both instances match
	 */
	public boolean sameAs(RelationConcept relationConcept) {
		return Objects.equals(getSourceURI(), relationConcept.getSourceURI());
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"RelationConcept\""
				+ ", \"parent\":\"" + super.toString() + '"'
				+ ", \"description\":\"" + description + '"'
				+ ", \"properties\":" + StringUtilities.setToJson(properties)
				+ '}';
	}

}
