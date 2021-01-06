package de.buw.tmdt.plasma.services.dms.core.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

@Configuration
@EnableMongoRepositories(basePackages = "de.buw.tmdt.plasma.services.dms")
public class MongoDatabaseConfiguration {

    @Bean
    public @NotNull MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(new CombinedModelSerializer(), new CombinedModelDeserializer()));
    }

    @Component
    @WritingConverter
    public class CombinedModelSerializer implements Converter<CombinedModel, String> {

        ObjectMapper mapper = getCustomObjectMapper();

        @Override
        public String convert(@NotNull CombinedModel model) {
            String result = null;
            try {
                result = mapper.writeValueAsString(model);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    @Component
    @ReadingConverter
    public class CombinedModelDeserializer implements Converter<String, CombinedModel> {

        ObjectMapper mapper = getCustomObjectMapper();

        @Override
        public CombinedModel convert(@NotNull String model) {
            CombinedModel result = null;
            try {
                result = mapper.readValue(model, CombinedModel.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public ObjectMapper getCustomObjectMapper() {
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