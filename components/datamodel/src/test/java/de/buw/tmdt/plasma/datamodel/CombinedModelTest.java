package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static de.buw.tmdt.plasma.datamodel.CombinedModelGenerator.getCombinedModel;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinedModelTest {


    @Test
    void testSerialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        CombinedModel model = getCombinedModel();

        String serialization = mapper.writeValueAsString(model);

        CombinedModel deserializedModel = mapper.readValue(serialization, CombinedModel.class);

        // TODO validate
    }

    @Test
    void testClone() {

        CombinedModel model = getCombinedModel();
        CombinedModel cloned = model.clone();

        assertEquals(model.getSyntaxModel().getRoot(), cloned.getSyntaxModel().getRoot());
        assertEquals(model.getSyntaxModel().getNodes().size(), cloned.getSyntaxModel().getNodes().size());
        assertEquals(model.getSyntaxModel().getEdges().size(), cloned.getSyntaxModel().getEdges().size());

        assertEquals(model.getSemanticModel().getEntityId(), cloned.getSemanticModel().getEntityId());
        assertEquals(model.getSemanticModel().getNodes().size(), cloned.getSemanticModel().getNodes().size());
        assertEquals(model.getSemanticModel().getEdges().size(), cloned.getSemanticModel().getEdges().size());

        assertEquals(model.getRecommendations().size(), cloned.getRecommendations().size());

        assertEquals(model.getPrimitiveEntityTypeMappings().size(), cloned.getPrimitiveEntityTypeMappings().size());

        assertEquals(model.getProvisionalEntityConcepts().size(), cloned.getProvisionalEntityConcepts().size());
        assertEquals(model.getProvisionalRelationConcepts().size(), cloned.getProvisionalRelationConcepts().size());

        // TODO deep testing
    }

    @Test
    void validate() {
        // TODO once implemented
    }
}