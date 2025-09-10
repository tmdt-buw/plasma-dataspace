package de.buw.tmdt.plasma.services.sms.kg;

import de.buw.tmdt.plasma.datamodel.CombinedModelElement;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticNode;
import de.buw.tmdt.plasma.services.sms.shared.model.ExportFormat;
import org.apache.commons.lang3.Strings;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class OntologyService {

    private static final Logger log = LoggerFactory.getLogger(OntologyService.class);
    private final OntologyManagement ontologyManagement;

    @Value("${plasma.kgs.localontology.label:LOCAL}")
    private String localOntologyLabel;

    @Autowired
    public OntologyService(
            OntologyManagement ontologyManagement) {
        this.ontologyManagement = ontologyManagement;
    }

    @NotNull
    public List<SemanticNode> getElements(@NotNull List<String> ontologyLabels,
                                          @Nullable String prefix,
                                          @Nullable String infix,
                                          @Nullable String uri
    ) {
        List<OntModel> filteredOntologyModels = ontologyManagement.getFilteredOntologyModels(ontologyLabels);
        List<SemanticNode> nodes = new ArrayList<>();
        if (uri != null && !uri.isBlank()) {
            findClasses(filteredOntologyModels, clazz -> uri.equals(clazz.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(filteredOntologyModels, ne -> uri.equals(ne.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .filter(Objects::nonNull)
                    .forEach(nodes::add);
        } else if (prefix != null && !prefix.isBlank()) {
            findClasses(filteredOntologyModels,
                    clazz -> Strings.CI.startsWith(clazz.getLabel(null), prefix)).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(filteredOntologyModels, ne -> Strings.CI.startsWith(ne.getProperty(RDFS.label) != null ? ne.getProperty(RDFS.label).getString() : "", prefix)).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .forEach(nodes::add);
        } else if (infix != null && !infix.isBlank()) {
            findClasses(filteredOntologyModels,
                    clazz -> Strings.CI.contains(clazz.getLabel(null), infix) ||
                            Strings.CI.contains(clazz.getURI(), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(filteredOntologyModels, ne -> Strings.CI.contains(ne.getProperty(RDFS.label) != null ? ne.getProperty(RDFS.label).getString() : "", infix) ||
                    Strings.CI.contains(ne.getURI(), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .filter(Objects::nonNull)
                    .forEach(nodes::add);
        } else {
            findClasses(filteredOntologyModels, clazz -> true).stream()
                    .map(CMToRDFMapper::convertOntologyClassToClass)
                    .forEach(nodes::add);
            findNamedEntities(filteredOntologyModels, ne -> true).stream()
                    .map(CMToRDFMapper::convertOntologyResourceToSemanticModelNode)
                    .filter(Objects::nonNull)
                    .forEach(nodes::add);
        }
        nodes.sort(Comparator.comparing(CombinedModelElement::getLabel));
        return nodes;
    }

    @NotNull
    public List<Relation> getRelations(@NotNull List<String> ontologyLabels, @Nullable String prefix, @Nullable String infix, String uri) {
        List<OntModel> filteredOntologyModels = ontologyManagement.getFilteredOntologyModels(ontologyLabels);
        List<Relation> relations = new ArrayList<>();
        if (uri != null && !uri.isBlank()) {
            findProperties(filteredOntologyModels, prop -> uri.equals(prop.getURI())).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .filter(Objects::nonNull)
                    .forEach(relations::add);
        } else if (prefix != null && !prefix.isBlank()) {
            findProperties(filteredOntologyModels,
                    prop -> Strings.CI.startsWith(prop.getLabel(null), prefix) ||
                            Strings.CI.startsWith(prop.getURI(), prefix)).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .filter(Objects::nonNull)
                    .forEach(relations::add);
        } else if (infix != null && !infix.isBlank()) {
            findProperties(filteredOntologyModels,
                    prop -> Strings.CI.contains(prop.getLabel(null), infix) ||
                            Strings.CI.contains(prop.getURI(), infix)).stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .filter(Objects::nonNull)
                    .forEach(relations::add);
        } else {
            List<OntProperty> properties = findProperties(filteredOntologyModels, prop -> true);
            properties.stream()
                    .map(CMToRDFMapper::convertOntologyPropertyToRelation)
                    .filter(Objects::nonNull)
                    .forEach(relations::add);
        }
        return relations;
    }

    private List<OntProperty> findProperties(List<OntModel> models, Predicate<OntProperty> filterFunction) {
        return models.stream()
                .flatMap(model -> model.listAllOntProperties().toList().stream()
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
                        .filter(subj -> subj.hasProperty(RDF.type, PLCM.NamedEntity))
                        .filter(filterFunction)
                )
                .collect(Collectors.toList());
    }

    public String downloadLocalOntology(ExportFormat format) {
        Optional<OntModel> ontologyModelByLabel = ontologyManagement.getOntologyModelByLabel(localOntologyLabel);
        if (ontologyModelByLabel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find a local ontology. Check with system maintainer.");
        }
        OntModel localOntology = ontologyModelByLabel.get();
        StringWriter out = new StringWriter();
        localOntology.write(out, format.getFormatString());
        return out.toString();
    }

    public void validateURI(String uri) {
        try {
            IRIs.checkEx(uri);
        } catch (IRIException ie) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "The URI provided is not valid according to RFC3987");
        }

        if (!getElements(List.of(localOntologyLabel), null, null, uri).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The URI provided is already in use in the local ontology");
        }

        if (!getRelations(List.of(localOntologyLabel), null, null, uri).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The URI provided is already in use in the local ontology");
        }
    }
}
