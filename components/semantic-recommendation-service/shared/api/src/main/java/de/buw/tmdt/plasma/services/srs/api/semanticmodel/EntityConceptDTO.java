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
	public static final String UUID_PROPERTY = "uuid";
	private static final String NAME_PROPERTY = "name";
	private static final String DESCRIPTION_PROPERTY = "description";

	private final Long id;
	private final String uuid;
	private final String name;
	private final String description;

	public EntityConceptDTO(@Nullable Long id, @Nullable String uuid, @NotNull String name, @NotNull String description) {
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.description = description;
	}

	@JsonCreator
	public EntityConceptDTO(
			@Nullable @JsonProperty(ID_PROPERTY) String id,
			@Nullable @JsonProperty(UUID_PROPERTY) String uuid,
			@NotNull @JsonProperty(NAME_PROPERTY) String name,
			@NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description
	) {
		this(
				id != null ? Long.parseLong(id) : null,
				uuid,
				name,
				description
		);
	}

	@JsonProperty(ID_PROPERTY)
	@Nullable
	public String getSerializedId() {
		return StringUtilities.toStringIfExists(id);
	}

	@JsonIgnore
	@Nullable
	public Long getId() {
		return id;
	}

	@JsonProperty(UUID_PROPERTY)
	@Nullable
	public String getSerializedUuid() {
		return StringUtilities.toStringIfExists(uuid);
	}

	@JsonIgnore
	@Nullable
	public String getUuid() {
		return uuid;
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
		       + ", \"uuid\":\"" + uuid + '"'
		       + ", \"name\":\"" + name + '"'
		       + ", \"description\":\"" + description + '"'
		       + '}';
	}
}