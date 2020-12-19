package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Relation extends CombinedModelElement implements Serializable {
	private static final long serialVersionUID = -2964347710102077197L;

	public static final String FROM_ID_PROPERTY = "from";
	public static final String TO_ID_PROPERTY = "to";
	private static final String DESCRIPTION_PROPERTY = "description";
	public static final String CONCEPT_PROPERTY = "concept";

	/**
	 * The uuid of the origin element, identified by CombinedModelElement#uuid
	 */
	private final String fromId;
	/**
	 * The uuid of the destination element, identified by CombinedModelElement#uuid
	 */
	private final String toId;
	private final String description;
	private final RelationConcept concept;

	public Relation(
			String fromId,
			String toId,
			String description,
			RelationConcept concept
	) {
		super("");
		this.fromId = fromId;
		this.toId = toId;
		this.description = description;
		this.concept = concept;
	}

	@JsonCreator
	public Relation(
			@NotNull @JsonProperty(UUID_PROPERTY) String uuid,
			@Nullable @JsonProperty(DATABASE_ID_PROPERTY) Long databaseId,
			@NotNull @JsonProperty(FROM_ID_PROPERTY) String fromId,
			@NotNull @JsonProperty(TO_ID_PROPERTY) String toId,
			@NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description,
			@NotNull @JsonProperty(CONCEPT_PROPERTY) RelationConcept concept
	) {
		super(uuid,databaseId,"");
				this.fromId = fromId;
				this.toId = toId;
				this.description = description;
				this.concept = concept;
	}

	@JsonProperty(FROM_ID_PROPERTY)
	public String getFromId() {
		return fromId;
	}

	@JsonProperty(TO_ID_PROPERTY)
	public String getToId() {
		return toId;
	}

	@JsonProperty(DESCRIPTION_PROPERTY)
	public String getDescription() {
		return description;
	}

	@JsonProperty(CONCEPT_PROPERTY)
	public RelationConcept getConcept() {
		return concept;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fromId, toId, description, concept);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !getClass().equals(o.getClass())) {
			return false;
		}
		Relation that = (Relation) o;
		return Objects.equals(fromId, that.fromId) &&
		       Objects.equals(toId, that.toId) &&
		       Objects.equals(description, that.description) &&
		       Objects.equals(concept, that.concept);
	}

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"RelationDTO\""
		       + ", \"fromId\":\"" + fromId + '"'
		       + ", \"toId\":\"" + toId + '"'
		       + ", \"description\":\"" + description + '"'
		       + ", \"concept\":" + concept
		       + '}';
	}
}
