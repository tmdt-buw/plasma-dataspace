package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	private static final String UUID_PROPERTY = "uuid";
	private static final String DESCRIPTION_PROPERTY = "description";
	private static final String SOURCE_URI_PROPERTY = "sourceURI";
	private static final String PROPERTIES_PROPERTY = "properties";

	private final String description;
	private final String sourceURI;
	private final Set<String> properties;

	public RelationConcept(
			@NotNull String label,
			@NotNull String description,
			@NotNull String sourceURI,
			@NotNull Set<String> properties
	) {
		super(label);
		this.description = description;
		this.sourceURI = sourceURI;
		this.properties = new HashSet<>(properties);
	}

	@JsonCreator
	public RelationConcept(
			@Nullable @JsonProperty(UUID_PROPERTY) String uuid,
			@Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
			@NotNull @JsonProperty(LABEL_PROPERTY) String label,
			@NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description,
			@NotNull @JsonProperty(SOURCE_URI_PROPERTY) String sourceURI,
			@NotNull @JsonProperty(PROPERTIES_PROPERTY) Set<String> properties
	) {
		super(				uuid,databaseId,label);
		this.description = description;
		this.sourceURI = sourceURI;
		this.properties = properties;
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

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"RelationConceptDTO\""
		       + ", \"parent\":\"" + super.toString() + '"'
		       + ", \"description\":\"" + description + '"'
		       + ", \"properties\":" + StringUtilities.setToJson(properties)
		       + '}';
	}
}
