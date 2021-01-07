package de.buw.tmdt.plasma.services.dms.core.database;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.TimeZone;

@Converter(autoApply = true)
public class CombinedModelPersistenceConverter implements AttributeConverter<CombinedModel, String> {

    private final ObjectMapper objectMapper = getObjectMapper();

    @Override
    public String convertToDatabaseColumn(CombinedModel combinedModel) {
        // clean up all the operations as they are not needed here
        combinedModel.getSyntaxModel().getNodes().forEach(
                schemaNode -> schemaNode.getOperations().clear()
        );
        try {
            return objectMapper.writeValueAsString(combinedModel);
        } catch (JsonProcessingException ex) {
            return null;
            // or throw an error
        }
    }

    @Override
    public CombinedModel convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, CombinedModel.class);
        } catch (IOException ex) {
            // logger.error("Unexpected IOEx decoding json from database: " + dbData);
            return null;
        }
    }

    public static ObjectMapper getObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .dateFormat(new StdDateFormat().withTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault())))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .featuresToEnable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

}
