package de.buw.tmdt.plasma.services.kgs.core;

import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.NamedEntity;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.update.Update;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.buw.tmdt.plasma.services.kgs.core.Ontologies.PREFIXES;

@Service
public class OntologyManagement {

    private RDFConnectionRemoteBuilder builder;


    public OntologyManagement(@Value("${plasma.jena.ontology.server}") String ontologyServerAddress) {
        builder = RDFConnectionFuseki.create()
                .destination(ontologyServerAddress);
    }

    public void addResourcesToOntology(List<SemanticModelNode> resources) {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(PREFIXES);
        for (SemanticModelNode resource : resources) {
            /* Enable this to only store plasma resources
            if(!clazz.getSourceURI().startsWith("plasma") && !clazz.getSourceURI().startsWith(PREFIXES.getNsPrefixURI("plasma"))){
                // this is not a prefix that belongs here
                continue;
            }
             */
            Resource r = m.createResource(m.expandPrefix(resource.getURI()));
            if (resource instanceof NamedEntity) {
                NamedEntity ne = (NamedEntity) resource;
                m.add(new StatementImpl(r, RDF.type, PLCM.NamedEntity));
                if (!resource.getLabel().isBlank()) {
                    m.add(new StatementImpl(r, RDFS.label, m.createLiteral(resource.getLabel())));
                }
                if (ne.getDescription() != null && !ne.getDescription().isBlank()) {
                    m.add(new StatementImpl(r, RDFS.comment, m.createLiteral(ne.getDescription())));
                }
            } else if (resource instanceof Class) {
                Class sClass = (Class) resource;
                if (!resource.getLabel().isBlank()) {
                    m.add(new StatementImpl(r, RDFS.label, m.createLiteral(resource.getLabel())));
                }
                if (sClass.getDescription() != null && !sClass.getDescription().isBlank()) {
                    m.add(new StatementImpl(r, RDFS.comment, m.createLiteral(sClass.getDescription())));
                }
            }
        }
        putModel(m);
    }

    public void addRelationsToOntology(List<Relation> relations) {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(PREFIXES);
        for (Relation relation : relations) {
            /* Enable this to only store plasma relations
            if(!relation.getSourceURI().startsWith("plasma") && !relation.getSourceURI().startsWith(PREFIXES.getNsPrefixURI("plasma"))){
                // this is not a prefix that belongs here
                continue;
            }
             */
            Resource r = m.createResource(m.expandPrefix(relation.getURI()));
            m.add(new StatementImpl(r, RDF.type, OWL.ObjectProperty));
            if (relation.getDescription() != null && !relation.getDescription().isBlank()) {
                m.add(new StatementImpl(r, RDFS.comment, m.createLiteral(relation.getDescription())));
            }
            if (!relation.getLabel().isBlank()) {
                m.add(new StatementImpl(r, RDFS.label, m.createLiteral(relation.getLabel())));
            }

        }
        putModel(m);
    }


    public SemanticModelNode findElementsByURI(String uri) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?class")
                    .addWhere("?class", RDF.type, "?type")
                    .addFilter(describeBuilder.getExprFactory().in("?type", OWL.Class, PLCM.NamedEntity))
                    .addFilter(describeBuilder.getExprFactory().eq("?class", ResourceFactory.createResource(PREFIXES.expandPrefix(uri))))
            //.addFilter(describeBuilder.getExprFactory().eq("?class", uri))
            ;

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<SemanticModelNode> elements = CMToRDFMapper.convertToSemanticModelNodes(model1.listSubjects().toList());
            return elements.stream().findFirst().orElse(null);
        }
    }


    public List<SemanticModelNode> findElementsByLabelPrefix(String prefix) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?class")
                    .addWhere("?class", RDF.type, OWL.Class)
                    .addWhere("?class", RDFS.label, "?o")
                    .addFilter(describeBuilder.getExprFactory().strstarts("?o", prefix));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<SemanticModelNode> elements = CMToRDFMapper.convertToSemanticModelNodes(model1.listSubjects().toList());
            return elements;
        }
    }

    public List<SemanticModelNode> findElementsByLabelInfix(String infix) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?class")
                    .addWhere("?class", RDF.type, OWL.Class)
                    .addWhere("?class", RDFS.label, "?o")
                    .addFilter(describeBuilder.getExprFactory().regex("?o", infix, "i"));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<SemanticModelNode> elements = CMToRDFMapper.convertToSemanticModelNodes(model1.listSubjects().toList());
            return elements;
        }
    }

    public List<SemanticModelNode> findElementsByNamespace(String namespace) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?class")
                    .addWhere("?class", RDF.type, OWL.Class)
                    .addFilter(describeBuilder.getExprFactory().strstarts("?class", namespace));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<SemanticModelNode> elements = CMToRDFMapper.convertToSemanticModelNodes(model1.listSubjects().toList());
            return elements;
        }
    }

    public Relation findRelationByURI(String uri) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?relation")
                    .addWhere("?relation", RDF.type, "?type")
                    .addFilter(describeBuilder.getExprFactory().in("?type", OWL.ObjectProperty, OWL.DatatypeProperty))
                    .addFilter(describeBuilder.getExprFactory().eq("?relation", ResourceFactory.createProperty(uri)))
            ;

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<Relation> elements = CMToRDFMapper.convertToRelations(model1.listSubjects().toList());
            return elements.stream().findFirst().orElse(null);
        }
    }


    public List<Relation> findRelationsByLabelPrefix(String prefix) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?relation")
                    .addWhere("?relation", RDFS.label, "?o")
                    .addWhere("?relation", RDF.type, "?type")
                    .addFilter(describeBuilder.getExprFactory().in("?type", OWL.ObjectProperty, OWL.DatatypeProperty))
                    .addFilter(describeBuilder.getExprFactory().strstarts("?o", prefix));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<Relation> elements = CMToRDFMapper.convertToRelations(model1.listSubjects().toList());
            return elements;
        }
    }

    public List<Relation> findRelationsByLabelInfix(String infix) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?relation")
                    .addWhere("?relation", RDFS.label, "?o")
                    .addWhere("?relation", RDF.type, "?type")
                    .addFilter(describeBuilder.getExprFactory().in("?type", OWL.ObjectProperty, OWL.DatatypeProperty))
                    .addFilter(describeBuilder.getExprFactory().strstarts("?o", infix));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<Relation> elements = CMToRDFMapper.convertToRelations(model1.listSubjects().toList());
            return elements;
        }
    }

    public List<Relation> findRelationsByNamespace(String namespace) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            DescribeBuilder describeBuilder = new DescribeBuilder();
            describeBuilder.addPrefixes(PREFIXES)
                    .addVar("?relation")
                    .addWhere("?relation", RDFS.label, "?o")
                    .addWhere("?relation", RDF.type, "?type")
                    .addFilter(describeBuilder.getExprFactory().in("?type", OWL.ObjectProperty, OWL.DatatypeProperty))
                    .addFilter(describeBuilder.getExprFactory().strstarts("?relation", namespace));

            QueryExecution query = conn.query(describeBuilder.build());
            Model model1 = query.execDescribe();
            List<Relation> elements = CMToRDFMapper.convertToRelations(model1.listSubjects().toList());
            return elements;
        }
    }

    private void putModel(Model model) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            UpdateBuilder updateBuilder = new UpdateBuilder()
                    .addPrefixes(PREFIXES)
                    .addInsert(model);
            Update insert = updateBuilder.build();
            conn.update(insert);
        }
    }
}
