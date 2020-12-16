package de.buw.tmdt.plasma.services.kgs.core.rdf;

import de.buw.tmdt.plasma.services.kgs.database.api.GraphDBDriverFactory;
import de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.EntityConceptRelation;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static de.buw.tmdt.plasma.services.kgs.core.rdf.UpperOntologyDefiner.URI_KGS_PREFIX;
import static de.buw.tmdt.plasma.services.kgs.core.rdf.UpperOntologyDefiner.URI_UO_PREFIX;

@Component
public class KgsToRdfConverter {

    private static final Logger logger = LoggerFactory.getLogger(KgsToRdfConverter.class);
    private GraphDBDriverFactory driverFactory;

    @Autowired
    public KgsToRdfConverter(@NotNull GraphDBDriverFactory driverFactory) {
        this.driverFactory = driverFactory;
    }

    @NotNull
    public OntModel convertKgsToRdf(boolean flat) {

        logger.info("Converting KG to rdf");

        Set<EntityConceptRelation> entityConceptRelations = driverFactory.getGraphDBDriver().retrieveEntityConceptRelations(false);

        OntModel model = UpperOntologyDefiner.getUpperOntologyModel();
        DatatypeProperty descriptionOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "description");
        DatatypeProperty uriOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "uri");
        DatatypeProperty labelOntProperty = model.getDatatypeProperty(URI_UO_PREFIX + "label");
        OntClass entityConceptOntClass = model.getOntClass(URI_UO_PREFIX + "EntityConcept");

        model.clearNsPrefixMap();
        model.setNsPrefixes(driverFactory.getGraphDBDriver().getPrefixMap());

        if (!flat) {
            model.setNsPrefix("tmdtkg", URI_KGS_PREFIX);
            model.setNsPrefix("tmdtuo", URI_UO_PREFIX);
        }

        for (EntityConceptRelation ecr : entityConceptRelations) {
            Resource from;
            Resource to;
            if (flat) {
                from = model.getResource(ecr.getTailNode().getSourceURI());

                to = model.getResource(ecr.getHeadNode().getSourceURI());

                if (from == null) {
                    from = model.createResource(ecr.getTailNode().getSourceURI());
                }
                if (to == null) {
                    to = model.createResource(ecr.getHeadNode().getSourceURI());
                }

                from.addProperty(
                        new PropertyImpl(ecr.getRelationConcept().getSourceURI()),
                        to
                );
            } else {

                StmtIterator fromIter = model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
                    @Override
                    public boolean selects(Statement statement) {
                        return statement.getObject().toString().equals(ecr.getTailNode().getId());
                    }
                });

                StmtIterator toIter = model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
                    @Override
                    public boolean selects(Statement statement) {
                        return statement.getObject().toString().equals(ecr.getHeadNode().getId());
                    }
                });

                if (!fromIter.hasNext()) {
                    from = model.createIndividual(URI_KGS_PREFIX + ecr.getTailNode().getMainLabel(), entityConceptOntClass)
                            .addProperty(labelOntProperty, ecr.getTailNode().getMainLabel())
                            .addProperty(uriOntProperty, ecr.getTailNode().getSourceURI())
                            .addProperty(descriptionOntProperty, ecr.getTailNode().getDescription());
                } else {
                    from = fromIter.next().getSubject();
                }

                if (!toIter.hasNext()) {
                    to = model.createIndividual(URI_KGS_PREFIX + ecr.getHeadNode().getMainLabel(), entityConceptOntClass)
                            .addProperty(labelOntProperty, ecr.getHeadNode().getMainLabel())
                            .addProperty(uriOntProperty, ecr.getHeadNode().getSourceURI())
                            .addProperty(descriptionOntProperty, ecr.getHeadNode().getDescription());
                } else {
                    to = toIter.next().getSubject();
                }

                from.addProperty(
                        new PropertyImpl(URI_KGS_PREFIX, ecr.getRelationConcept().getLabel()),
                        to
                );

                model.remove(model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
                    @Override
                    public boolean selects(Statement statement) {
                        return statement.getObject().toString().equals(ecr.getHeadNode().getId());
                    }
                }));
            }
        }

        model.remove(UpperOntologyDefiner.getUpperOntologyModel());

        logger.info("Converted KG to RDF successfully");

        return model;
    }
}
