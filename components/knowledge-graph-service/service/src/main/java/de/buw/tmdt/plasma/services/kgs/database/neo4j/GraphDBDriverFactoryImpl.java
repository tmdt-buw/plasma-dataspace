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
        return this.driver;
    }

    public void closeDriver() {
    }

    public void executeCypher(@NotNull String query) {
        try (Session session = openDriver().session(); Transaction transaction = session.beginTransaction()) {
            transaction.run(query);
            transaction.commit();
        }
    }
}