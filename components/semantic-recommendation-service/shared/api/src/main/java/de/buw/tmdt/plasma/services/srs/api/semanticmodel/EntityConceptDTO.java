package de.buw.tmdt.plasma.services.srs.api.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.utilities.misc.StringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class EntityConceptDTO implements Serializable {
	private static final long serialVersionUID = 2351939502355153654L;
	public static final String ID_PROPERTY = "id";
	private static final String NAME_PROPERTY = "name";
	private static final String DESCRIPTION_PROPERTY = "description";
	private static final String URI_PROPERTY = "uri";

	private final String id;
	private final String name;
	private final String description;
	private final String uri;

	@JsonCreator
	public EntityConceptDTO(
			@NotNull @JsonProperty(ID_PROPERTY) String id,
			@NotNull @JsonProperty(NAME_PROPERTY) String name,
			@NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description,
			@NotNull @JsonProperty(URI_PROPERTY) String uri
	) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.uri = uri;
	}

	@JsonProperty(ID_PROPERTY)
	@Nullable
	public String getSerializedId() {
		return StringUtilities.toStringIfExists(id);
	}

	@JsonIgnore
	@Nullable
	public String getId() {
		return id;
	}

	@JsonProperty(NAME_PROPERTY)
	@NotNull
	public String getName() {
		return this.name;
	}

	@JsonProperty(DESCRIPTION_PROPERTY)
	@NotNull
	public String getDescription() {
		return description;
	}

	@JsonProperty(URI_PROPERTY)
	@NotNull
	public String getUri() {
		return uri;
	}

	public int hashCode() {
		return this.name.hashCode();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass().equals(o.getClass())) {
			EntityConceptDTO that = (EntityConceptDTO) o;
			return this.name.equals(that.name);
		} else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"EntityConceptDTO\""
		       + ", \"id\":\"" + id + '"'
		       + ", \"name\":\"" + name + '"'
		       + ", \"description\":\"" + description + '"'
		       + '}';
	}

}