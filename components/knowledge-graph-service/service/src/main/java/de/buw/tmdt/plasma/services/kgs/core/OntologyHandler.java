package de.buw.tmdt.plasma.services.kgs.core;

import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.buw.tmdt.plasma.services.kgs.core.Ontologies.PREFIXES;

@Service
public class OntologyHandler {

    private static final Logger log = LoggerFactory.getLogger(OntologyHandler.class);
    private final LocalOntologyManagement onto;
    private final Ontologies ontologies;

    @Autowired
    public OntologyHandler(
            @NotNull LocalOntologyManagement onto, Ontologies ontologies) {
        this.onto = onto;
        this.ontologies = ontologies;
    }

    @NotNull
    public List<SemanticModelNode> getElements(@NotNull List<String> ontologyLabels,
                                               @Nullable String prefix,
                                               @Nullable String infix,
                                               @Nullable String uri
    ) {
        List<OntModel> localOntologyList = ontologies.getFilteredOntologyModels(Collections.singletonList("PLASMA"));

        List<OntModel> filteredOntologyModels = ontologies.getFilteredOntologyModels(ontologyLabels);
        List<SemanticModelNode> nodes = new ArrayList<>();
        if (uri != null && !uri.isBlank()) {
            findClasses(filteredOntologyModels, clazz -> uri.equals(clazz.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(localOntologyList, ne -> uri.equals(ne.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .forEach(nodes::add);
        } else if (prefix != null && !prefix.isBlank()) {
            findClasses(filteredOntologyModels,
                    clazz -> StringUtils.startsWithIgnoreCase(clazz.getLabel(null), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(localOntologyList, ne -> StringUtils.startsWithIgnoreCase(ne.getRequiredProperty(RDFS.label).getString(), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .forEach(nodes::add);
        } else if (infix != null && !infix.isBlank()) {
            findClasses(filteredOntologyModels,
                    clazz -> StringUtils.containsIgnoreCase(clazz.getLabel(null), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(localOntologyList, ne -> StringUtils.containsIgnoreCase(ne.getRequiredProperty(RDFS.label).getString(), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .forEach(nodes::add);
        } else {
            findClasses(filteredOntologyModels, clazz -> Objects.nonNull(clazz.getLabel(null))).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
        }
        nodes.sort(Comparator.comparing(CombinedModelElement::getLabel));
        return nodes;
    }

    @NotNull
    public List<Relation> getRelations(@NotNull List<String> ontologyLabels, @Nullable String prefix, @Nullable String infix, String uri) {
        List<OntModel> filteredOntologyModels = ontologies.getFilteredOntologyModels(ontologyLabels);
        List<Relation> relations = new ArrayList<>();
        if (uri != null && !uri.isBlank()) {
            findProperties(filteredOntologyModels, prop -> uri.equals(prop.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .forEach(relations::add);
        } else if (prefix != null && !prefix.isBlank()) {
            findProperties(filteredOntologyModels,
                    prop -> StringUtils.startsWithIgnoreCase(prop.getLabel(null), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .forEach(relations::add);
        } else if (infix != null && !infix.isBlank()) {
            findProperties(filteredOntologyModels,
                    prop -> StringUtils.containsIgnoreCase(prop.getLabel(null), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .forEach(relations::add);
        } else {
            findProperties(filteredOntologyModels, prop -> Objects.nonNull(prop.getLabel(null))).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .forEach(relations::add);
        }
        return relations;
    }

    private List<OntProperty> findProperties(List<OntModel> models, Predicate<OntProperty> filterFunction) {
        return models.stream()
                .flatMap(model -> model.listOntProperties().toList().stream()
                        .filter(filterFunction)
                )
                .collect(Collectors.toList());
    }

    private List<OntClass> findClasses(List<OntModel> models, Predicate<OntResource> filterFunction) {
        return models.stream()
                .flatMap(model -> model.listNamedClasses().toList().stream()
                        .filter(filterFunction)
                )
                .collect(Collectors.toList());
    }

    private List<Resource> findNamedEntities(List<OntModel> models, Predicate<Resource> filterFunction) {
        return models.stream()
                .flatMap(model -> model.listSubjects().toList().stream()
                        .filter(subj -> subj.hasProperty(RDF.type, PLASMA.NamedEntity))
                        .filter(filterFunction)
                )
                .collect(Collectors.toList());
    }


}