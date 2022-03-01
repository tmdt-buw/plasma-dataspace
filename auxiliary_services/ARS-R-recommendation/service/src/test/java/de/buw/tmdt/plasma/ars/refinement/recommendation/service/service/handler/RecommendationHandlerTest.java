package de.buw.tmdt.plasma.ars.refinement.recommendation.service.service.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Disabled
class RecommendationHandlerTest {

    @Autowired
    RecommendationHandler handler;

    @Test
    void generateRecommendations() {
        List<SemanticModelNode> entities = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();

        String ref = "plasma:latitude";
        Class objectClass = new Class(ref, "latitude", "");
        entities.add(objectClass);
        CombinedModel cm = new CombinedModel("test", new SyntaxModel(), new SemanticModel(null, entities, relations));
        CombinedModel combinedModel = handler.generateRecommendations(cm);
        assertTrue(combinedModel.getRecommendations().size() > 0);
    }
}