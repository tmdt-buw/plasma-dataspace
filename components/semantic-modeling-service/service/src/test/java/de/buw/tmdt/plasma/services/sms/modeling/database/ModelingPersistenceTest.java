package de.buw.tmdt.plasma.services.sms.modeling.database;

import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import de.buw.tmdt.plasma.services.sms.modeling.model.Modeling;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PLASMA_TEST_DATABASE", matches = "true")
@SpringBootTest(classes = ModelingPersistenceTest.Config.class, properties = {
        "spring.datasource.username=cleanup", "spring.datasource.password=cleanup-test-only",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false"
})
class ModelingPersistenceTest {
    @SpringBootConfiguration @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Modeling.class)
    @EnableJpaRepositories(basePackageClasses = ModelingRepository.class)
    static class Config { }
    @DynamicPropertySource
    static void disposableDatabase(DynamicPropertyRegistry properties) {
        String url = System.getenv("PLASMA_TEST_DATABASE_URL");
        if (url == null || !url.matches("jdbc:mariadb://(127\\.0\\.0\\.1|localhost|cleanup-db|database)(:[0-9]+)?/plasma_cleanup_test")) {
            throw new IllegalArgumentException("A disposable local plasma_cleanup_test database is required");
        }
        // Validate before Spring opens a connection or Hibernate creates/drops tables.
        properties.add("spring.datasource.url", () -> url);
    }
    @Autowired ModelingRepository repository;
    @Autowired EntityManager entityManager;
    @Test @Transactional void persistsHistoryAndCurrentVersion() {
        Modeling modeling = new Modeling(); modeling.setName("Isolated regression test");
        var generator = new AdvancedCombinedModelGenerator();
        for (int i = 0; i < 3; i++) modeling.pushCombinedModel(generator.getFlightModel());
        String current = modeling.getCurrentModel().getId();
        repository.saveAndFlush(modeling); entityManager.clear();
        Modeling restored = repository.findById(modeling.getId()).orElseThrow();
        assertNotSame(modeling, restored);
        assertEquals(3, restored.getAllCombinedModels().size());
        assertEquals(current, restored.getCurrentModel().getId());
        assertEquals("Isolated regression test", restored.getName());
        assertEquals(modeling.getCurrentModel().getSyntaxModel().getNodes().size(), restored.getCurrentModel().getSyntaxModel().getNodes().size());
    }
}
