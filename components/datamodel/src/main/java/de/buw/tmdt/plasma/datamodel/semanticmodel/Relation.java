package de.buw.tmdt.plasma.datamodel.semanticmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class Relation extends CombinedModelElement implements Serializable {
	private static final long serialVersionUID = -2964347710102077197L;

	public static final String ENTITY_ID_PROPERTY = "entityId";
	public static final String FROM_ID_PROPERTY = "from";
	public static final String TO_ID_PROPERTY = "to";
	private static final String DESCRIPTION_PROPERTY = "description";
	public static final String CONCEPT_PROPERTY = "concept";

	/**
	 * Knowledge base ID, set once sent to the KGS
	 */
	private String entityId;

	/**
	 * The uuid of the origin element, identified by CombinedModelElement#uuid
	 */
	private String fromId;
	/**
	 * The uuid of the destination element, identified by CombinedModelElement#uuid
	 */
	private  String toId;
	private String description;
	private RelationConcept concept;

	public Relation(
			String fromId,
			String toId,
			RelationConcept concept
	) {
		this(fromId,toId,"",concept);
	}

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
			@NotNull @JsonProperty(FROM_ID_PROPERTY) String fromId,
			@NotNull @JsonProperty(TO_ID_PROPERTY) String toId,
			@NotNull @JsonProperty(DESCRIPTION_PROPERTY) String description,
			@NotNull @JsonProperty(CONCEPT_PROPERTY) RelationConcept concept
	) {
		super(uuid,"");
				this.fromId = fromId;
				this.toId = toId;
				this.description = description;
				this.concept = concept;
	}

	@JsonProperty(ENTITY_ID_PROPERTY)
	public String getEntityId() {
		return entityId;
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

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public void setFromId(String fromId) {
		this.fromId = fromId;
	}

	public void setToId(String toId) {
		this.toId = toId;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setConcept(RelationConcept concept) {
		this.concept = concept;
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
		return "{\"@class\":\"Relation\""
				+ ", \"fromId\":\"" + fromId + '"'
				+ ", \"toId\":\"" + toId + '"'
				+ ", \"description\":\"" + description + '"'
				+ ", \"concept\":" + concept
				+ '}';
	}
}
