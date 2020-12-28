package de.buw.tmdt.plasma.datamodel;

import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.datamodel.semanticmodel.EntityType;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationConcept;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import static de.buw.tmdt.plasma.datamodel.CombinedModelGenerator.getCombinedModel;
import static org.junit.jupiter.api.Assertions.*;

public class CombinedModelSemanticModificationTest {

    @Test
    public void testApplyFailsIfFinalized() {
        CombinedModel model = getCombinedModel();
        model.finalizeModel();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        assertThrows(UnsupportedOperationException.class, () -> model.apply(modification));
    }

    @Test
    public void testApplyAddOneEntity() {
        CombinedModel model = getCombinedModel();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntity", Collections.singleton(companyType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyType), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }

    @Test
    public void testApplyAddOneEntityAndOneRelation() {
        CombinedModel model = getCombinedModel();

        EntityType airlineType = model.getSemanticModel().getNodes().stream().filter(entityType -> entityType.getEntityConcept().getSourceURI().equals("plasma:airline")).findFirst().orElseThrow();

        EntityConcept companyConcept = new EntityConcept("company", "A company", "plasma:company");
        EntityType companyType = new EntityType(companyConcept.getLabel(), "", companyConcept);

        RelationConcept isARelationConcept = new RelationConcept("isA", "Indicates that something is a superclass of something", "plasma:isA", new HashSet<>());
        Relation airlineCompanyRelation = new Relation(airlineType.getUuid(), companyType.getUuid(), isARelationConcept);

        DeltaModification modification = new DeltaModification("mod_testApplyAddOneEntityAndOneRelation", Collections.singleton(companyType), Collections.singleton(airlineCompanyRelation), null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size() + 1, model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");
        assertTrue(model.getSemanticModel().getNodes().contains(companyType), "New entity type is not available");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size() + 1, model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
        assertTrue(model.getSemanticModel().getEdges().contains(airlineCompanyRelation), "New relation is not available");
    }

    @Test
    public void testApplyReplaceOneEntity() {
        CombinedModel model = getCombinedModel();

        EntityType iataCodeType = model.getSemanticModel().getNodes().stream().filter(entityType -> entityType.getEntityConcept().getSourceURI().equals("plasma:iata_code")).findFirst().orElseThrow();

        EntityConcept codeConcept = new EntityConcept("code", "A sort of identifier or short version of text", "plasma:code");
        EntityType codeType = new EntityType(iataCodeType.getUuid(),
                iataCodeType.getEntityId(),
                "code",
                iataCodeType.getSyntaxNodeUuid(),
                iataCodeType.getOriginalLabel(),
                "The code of this airline",
                codeConcept,
                iataCodeType.getXCoordinate(),
                iataCodeType.getYCoordinate());

        DeltaModification modification = new DeltaModification("mod_testApplyReplaceOneEntity", Collections.singleton(codeType), null, null, null, 0.0);

        model.apply(modification);

        assertEquals(getCombinedModel().getSemanticModel().getNodes().size(), model.getSemanticModel().getNodes().size(), "Number of expected entities does not match");

        EntityType replacedEntity = model.getSemanticModel().getNodes().stream().filter(entityType -> Objects.equals(entityType.getUuid(), codeType.getUuid())).findFirst().orElseThrow();
        assertEquals(replacedEntity.getUuid(), iataCodeType.getUuid(),"Replacement type does not have the same id as replaced entity");
        assertEquals(codeConcept.getSourceURI(),replacedEntity.getEntityConcept().getSourceURI(), "New entity concept is not assigned");
        assertEquals(codeType.getDescription(),replacedEntity.getDescription(), "New entity description is not applied correctly");
        assertTrue(model.getSemanticModel().getNodes().stream().noneMatch(entityType -> entityType.getLabel().equals(iataCodeType.getLabel())), "Old entity type is not completely replaced");
        assertTrue(model.getSemanticModel().getNodes().stream().noneMatch(entityType -> entityType.getEntityConcept().getSourceURI().equals(iataCodeType.getEntityConcept().getSourceURI())), "Old entity concept is still lingering");
        assertEquals(getCombinedModel().getSemanticModel().getEdges().size(), model.getSemanticModel().getEdges().size(), "Number of expected relations does not match");
    }


}