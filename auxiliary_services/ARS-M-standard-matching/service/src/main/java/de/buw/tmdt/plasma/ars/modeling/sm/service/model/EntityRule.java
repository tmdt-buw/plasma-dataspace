package de.buw.tmdt.plasma.ars.modeling.sm.service.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class EntityRule {

	@Id
	@Column(nullable = false, unique = true, length = 16)
	protected UUID uuid;

	@Column
	private String originalAttribute;

	@Column
	private String conceptName;

	@Column
	private String conceptDescription;

	@Column
	private String requiredPredecessor;

	@Column
	private String conceptSourceURI;

	protected EntityRule() {
	}

	public EntityRule(@Nullable String originalAttribute,
	                  @NotNull String conceptName,
	                  @NotNull String conceptDescription,
	                  @NotNull String conceptSourceURI,
	                  @Nullable String requiredPredecessor) {
		this.conceptSourceURI = conceptSourceURI;
		this.uuid = UUID.randomUUID();
		this.originalAttribute = originalAttribute;
		this.conceptName = conceptName;
		this.conceptDescription = conceptDescription;
		this.requiredPredecessor = requiredPredecessor;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getOriginalAttribute() {
		return originalAttribute;
	}

	public String getConceptName() {
		return conceptName;
	}

	public String getConceptDescription() {
		return conceptDescription;
	}

	public String getRequiredPredecessor() {
		return requiredPredecessor;
	}

	public String getConceptSourceURI() {
		return conceptSourceURI;
	}
}
