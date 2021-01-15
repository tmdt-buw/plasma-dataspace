package de.buw.tmdt.plasma.ars.modeling.sm.service.model;

import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class RelationRule {

    @Id
    @Column(nullable = false, unique = true, length = 16)
    protected UUID uuid;

    @Column
    private String fromConceptSourceURI;

    @Column
    private String toConceptSourceURI;

    @Column
    private String conceptName;

    @Column
    private String conceptDescription;

    @Column
    private String conceptSourceURI;

    protected RelationRule() {
    }

    public RelationRule(@NotNull String fromConcept, @NotNull String toConcept, @NotNull String conceptName, @NotNull String conceptDescription, @NotNull String conceptSourceURI) {
        this.conceptSourceURI = conceptSourceURI;
        this.uuid = UUID.randomUUID();
        this.fromConceptSourceURI = fromConcept;
        this.toConceptSourceURI = toConcept;
        this.conceptName = conceptName;
        this.conceptDescription = conceptDescription;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getFromConceptSourceURI() {
        return fromConceptSourceURI;
    }

    public String getToConceptSourceURI() {
        return toConceptSourceURI;
    }

    public String getConceptName() {
        return conceptName;
    }

    public String getConceptDescription() {
        return conceptDescription;
    }

    public String getConceptSourceURI() {
        return conceptSourceURI;
    }
}
