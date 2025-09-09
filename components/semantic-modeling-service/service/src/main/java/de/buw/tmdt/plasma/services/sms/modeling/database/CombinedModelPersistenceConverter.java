package de.buw.tmdt.plasma.services.sms.modeling.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Custom converter to store CombinedModels in a serialized format.
 */
@Converter(autoApply = true)
public class CombinedModelPersistenceConverter implements AttributeConverter<CombinedModel, String> {

    private static final Logger log = LoggerFactory.getLogger(CombinedModelPersistenceConverter.class);

    private final ObjectMapper objectMapper = getObjectMapper();

    @Override
    public String convertToDatabaseColumn(CombinedModel combinedModel) {
        // clean up all the operations as they are not needed here
        try {
            return objectMapper.writeValueAsString(combinedModel);
        } catch (JsonProcessingException ex) {
            log.error("Unexpected IOEx encoding json to database: ", ex);
            return null;
            // or throw an error
        }
    }

    @Override
    public CombinedModel convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, CombinedModel.class);
        } catch (IOException ex) {
            log.error("Unexpected IOEx decoding json from database: " + dbData, ex);
            return null;
        }
    }

    public static ObjectMapper getObjectMapper() {

        ObjectMapper mapper = JsonMapper.builder()
                .defaultDateFormat(new StdDateFormat().withTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault())))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .build();

        mapper.setMixInResolver(new ClassIntrospector.MixInResolver() {
            @Override
            public Class<?> findMixInClassFor(Class<?> cls) {
                return ModelMappingsIgnoreMixIn.class;
            }

            @Override
            public ClassIntrospector.MixInResolver copy() {
                return this;
            }
        });
        return mapper;
    }

    @JsonIgnoreProperties({"modelMappings"})
    abstract static class ModelMappingsIgnoreMixIn {
    }


}
