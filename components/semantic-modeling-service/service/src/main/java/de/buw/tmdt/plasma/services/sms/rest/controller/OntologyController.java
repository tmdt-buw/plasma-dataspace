package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.datamodel.semanticmodel.NodeTemplate;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticNode;
import de.buw.tmdt.plasma.services.sms.kg.OntologyService;
import de.buw.tmdt.plasma.services.sms.kg.OntologyManagement;
import de.buw.tmdt.plasma.services.sms.shared.api.OntologyApi;
import de.buw.tmdt.plasma.services.sms.shared.model.ExportFormat;
import de.buw.tmdt.plasma.services.sms.shared.model.OntologyInfo;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class OntologyController implements OntologyApi {

    private static final Logger log = LoggerFactory.getLogger(OntologyController.class);
    private final OntologyService ontologyService;
    private final OntologyManagement ontologyManagement;

    @Value("${plasma.kgs.localontology.label:LOCAL}")
    private String localOntologyLabel;

    @Autowired
    public OntologyController(@NotNull OntologyService ontologyService,
                              @NotNull OntologyManagement ontologyManagement) {
        this.ontologyService = ontologyService;
        this.ontologyManagement = ontologyManagement;
    }

    @Override
    public @NotNull List<SemanticNode> getElements(List<String> ontologyLabels, String prefix, String infix, String uri, long limit) {
        List<SemanticNode> elements = ontologyService.getElements(ontologyLabels, prefix, infix, uri);
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(element -> ((NodeTemplate) element).convertToTemplate());
        return elements.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public @NotNull List<Relation> getRelations(List<String> ontologyLabels, String prefix, String infix, String uri, long limit) {
        List<Relation> relations = ontologyService.getRelations(ontologyLabels, prefix, infix, uri);
        return relations.stream()
                .filter(Objects::nonNull)
                .limit(limit)
                .map(Relation::convertToTemplate)
                .collect(Collectors.toList());
    }

    @Override
    public void addOntology(String label, String prefix, String uri, String description, MultipartFile ontologyFile) {
        ontologyManagement.addOntology(label, prefix, uri, description, ontologyFile);
    }

    @Override
    public void deleteOntology(String label) {
        ontologyManagement.deleteOntology(label);
    }

    @Override
    public List<OntologyInfo> listOntologies() {
        return ontologyManagement.listOntologies();
    }

    @Override
    public ResponseEntity<String> downloadLocalOntology(@NotNull ExportFormat format) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(localOntologyLabel + ".ttl")
                .build();
        headers.setContentDisposition(contentDisposition);
        headers.set("filename", localOntologyLabel + ".ttl");
        headers.setAccessControlExposeHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, "filename"));

        return new ResponseEntity<>(ontologyService.downloadLocalOntology(format), headers, HttpStatus.OK);
    }

    @Override
    public Map<String, String> getNamespaces() {
        return OntologyManagement.PREFIXES.getNsPrefixMap();
    }

    @Override
    public void validateURI(String uri) {
        ontologyService.validateURI(uri);
    }
}