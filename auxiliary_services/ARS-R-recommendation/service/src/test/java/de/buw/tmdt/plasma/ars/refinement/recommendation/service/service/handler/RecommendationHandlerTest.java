package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RecommendationHandlerTest {

    @Autowired
    RecommendationHandler handler;

    @Test
    void generateRecommendations() {
        List<EntityType> entities = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();

        String ref = "plasma:latitude";

        RelationConcept rc = new RelationConcept(ref, "", ref);
        Relation r = new Relation(ref, "", rc);

        EntityConcept o_ec = new EntityConcept(ref, "", ref);
        EntityType o_et = new EntityType(ref, "", o_ec);

        entities.add(o_et);
        relations.add(r);
        CombinedModel cm = new CombinedModel("test", new SyntaxModel(), new SemanticModel(null, entities, relations));
        CombinedModel combinedModel = handler.generateRecommendations(cm);
        assertTrue(combinedModel.getRecommendations().size() > 0);

    }
}