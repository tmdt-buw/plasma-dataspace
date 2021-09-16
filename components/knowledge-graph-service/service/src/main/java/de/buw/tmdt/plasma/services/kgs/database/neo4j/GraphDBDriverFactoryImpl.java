package de.buw.tmdt.plasma.services.kgs.database.neo4j;

import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriver;
import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriverFactory;
import org.jetbrains.annotations.NotNull;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphDBDriverFactoryImpl implements GraphDBDriverFactory {

    private static final long serialVersionUID = -5117991571995417971L;

    private final Neo4JProfile neo4jDBProfile;
    private final Driver driver;

    @Autowired
    public GraphDBDriverFactoryImpl(@NotNull Neo4JProfile neo4jDBProfile, Driver driver) {
        this.neo4jDBProfile = neo4jDBProfile;
        this.driver = driver;
    }

    @NotNull
    @Override
    public GraphDBDriver getGraphDBDriver() {
        return new GraphDBDriverImpl(openDriver());
    }

    @NotNull
    public Driver openDriver() {
        // Create driver instance.
//        if (this.driver == null) {
//            Config config = Config.builder()
//                    .withMaxConnectionPoolSize(neo4jDBProfile.getMaxIdleSessions())
//                    .withConnectionAcquisitionTimeout(neo4jDBProfile.getTimeoutMs(), TimeUnit.MILLISECONDS)
//                    .build();
//            this.driver = GraphDatabase.driver(
//                    neo4jDBProfile.getUri(),
//                    AuthTokens.basic(neo4jDBProfile.getUsername(), neo4jDBProfile.getPassword()),
//                    config
//            );
//        }
        return this.driver;
    }

    public void closeDriver() {
        // Create driver instance.
//        if (this.driver != null) {
//            this.driver.close();
//        }
    }

    public void executeCypher(@NotNull String query) {
        try (Session session = openDriver().session(); Transaction transaction = session.beginTransaction()) {
            transaction.run(query);
            transaction.commit();
        }
    }
}