package de.buw.tmdt.plasma.services.kgs.core.rdf;

import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriverFactory;
import de.buw.tmdt.plasma.services.kgs.shared.model.semanticmodel.EntityType;
import de.buw.tmdt.plasma.services.kgs.shared.model.semanticmodel.Relation;
import de.buw.tmdt.plasma.services.kgs.shared.model.semanticmodel.SemanticModel;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static de.buw.tmdt.plasma.services.kgs.core.rdf.UpperOntologyDefiner.*;

@SuppressWarnings("HardcodedFileSeparator")
@Component
public class ModelToRdfConverter {

    private static final Logger logger = LoggerFactory.getLogger(ModelToRdfConverter.class);
    private GraphDBDriverFactory driverFactory;

    @Autowired
    public ModelToRdfConverter(@NotNull GraphDBDriverFactory driverFactory) {
        this.driverFactory = driverFactory;
    }

    @NotNull
    public OntModel convertSemanticModelToRdf(@NotNull String id, boolean flat) {

        SemanticModel semanticModel = driverFactory.getGraphDBDriver().retrieveSemanticModel(id);

        Set<Relation> relationSet = semanticModel.getRelations();
        Set<EntityType> entityTypes = semanticModel.getEntityTypes();

        OntModel model = UpperOntologyDefiner.getUpperOntologyModel();
        ObjectProperty relationOntProperty = model.getObjectProperty(URI_UO_PREFIX + "instantiatedBy");
        DatatypeProperty descriptionOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "description");
        DatatypeProperty uriOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "uri");
        DatatypeProperty labelOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "label");
        OntClass entityConceptOntClass = model.getOntClass(URI_UO_PREFIX + "EntityConcept");
        OntClass entityTypeOntClass = model.getOntClass(URI_UO_PREFIX + "EntityType");

        Map<String, String> kgsPrefixMap = driverFactory.getGraphDBDriver().getPrefixMap();

        model.clearNsPrefixMap();

        if (!flat) {
            model.setNsPrefix("tmdtkg", URI_KGS_PREFIX);
            model.setNsPrefix("tmdtuo", URI_UO_PREFIX);
            model.setNsPrefix("tmdtsm", URI_SM_PREFIX);
        }

        for (EntityType et : entityTypes) {
            if (flat) {
                // Individual prefix import so as to not import the whole prefix map
                String ecUriAndLabel = et.getEntityConcept().getSourceURI().replace(et.getEntityConcept().getMainLabel(), "");
                if (!getPrefixForUrl(kgsPrefixMap, ecUriAndLabel).isEmpty()) {
                    model.setNsPrefix(getPrefixForUrl(kgsPrefixMap, ecUriAndLabel), ecUriAndLabel);
                }
                model.createResource(et.getEntityConcept().getSourceURI());
            } else {
                Resource entityConceptIndividual = model.createIndividual(URI_KGS_PREFIX + et.getEntityConcept().getMainLabel(), entityConceptOntClass)
                        .addProperty(labelOntProperty, et.getEntityConcept().getMainLabel())
                        .addProperty(uriOntProperty, et.getEntityConcept().getSourceURI())
                        .addProperty(descriptionOntProperty, et.getEntityConcept().getDescription());

                model.createIndividual(URI_SM_PREFIX + et.getLabel(), entityTypeOntClass)
                        .addProperty(labelOntProperty, et.getLabel())
                        .addProperty(descriptionOntProperty, et.getDescription())
                        .addProperty(relationOntProperty, entityConceptIndividual);
            }
        }

        for (Relation rel : relationSet) {
            if (flat) {
                Resource from = model.getResource(rel.getTailNode().getEntityConcept().getSourceURI());

                Resource to = model.getResource(rel.getHeadNode().getEntityConcept().getSourceURI());

                // Individual prefix import so as to not import the whole prefix map
                String relUrlAndLabel = rel.getRelationConcept().getSourceURI().replace(rel.getRelationConcept().getLabel(), "");
                if (!getPrefixForUrl(kgsPrefixMap, relUrlAndLabel).isEmpty()) {
                    model.setNsPrefix(getPrefixForUrl(kgsPrefixMap, relUrlAndLabel), relUrlAndLabel);
                }

                model.createResource(rel.getRelationConcept().getSourceURI());

                from.addProperty(
                        new PropertyImpl(rel.getRelationConcept().getSourceURI()),
                        to
                );
            } else {

                Resource from = model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
                    @Override
                    public boolean selects(Statement statement) {
                        return statement.getSubject().getClass().isInstance(entityConceptOntClass) && statement.getObject().toString()
                                .equals(rel.getTailNode().getLabel());
                    }
                }).nextStatement().getSubject();

                Resource to = model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
                    @Override
                    public boolean selects(Statement statement) {
                        return statement.getSubject().getClass().isInstance(entityConceptOntClass) && statement.getObject().toString()
                                .equals(rel.getHeadNode().getLabel());
                    }
                }).nextStatement().getSubject();

                from.addProperty(
                        new PropertyImpl(URI_KGS_PREFIX, rel.getRelationConcept().getLabel()),
                        to
                );
            }
        }

        model.remove(UpperOntologyDefiner.getUpperOntologyModel());
        return model;
    }

    private String getPrefixForUrl(Map<String, String> prefixMap, String url) {
        for (Map.Entry<String, String> entry : prefixMap.entrySet()) {
            if (entry.getValue().equals(url)) {
                return entry.getKey();
            }
        }
        logger.info("No prefix found for url: {}", url);
        return "";
    }
}
