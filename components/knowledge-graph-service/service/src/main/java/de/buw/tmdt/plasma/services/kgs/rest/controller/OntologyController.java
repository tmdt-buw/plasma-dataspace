package de.buw.tmdt.plasma.services.kgs.rest.controller;

import de.buw.tmdt.plasma.datamodel.AsTemplate;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.URIDefinedElement;
import de.buw.tmdt.plasma.services.kgs.core.OntologyHandler;
import de.buw.tmdt.plasma.services.kgs.shared.api.OntologyApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OntologyController implements OntologyApi {

    private final OntologyHandler ontologyHandler;

    @Autowired
    public OntologyController(@NotNull OntologyHandler ontologyHandler) {
        this.ontologyHandler = ontologyHandler;
    }

    @Override
    public @NotNull List<URIDefinedElement> getElements(String prefix, String infix, String uri) {
        List<URIDefinedElement> elements = ontologyHandler.getElements(prefix, infix, uri);
        elements.forEach(AsTemplate::convertToTemplate);
        return elements;
    }

    @Override
    public @NotNull List<Relation> getRelations(String prefix, String infix, String uri) {
        List<Relation> relations = ontologyHandler.getRelations(prefix, infix, uri);
        relations.forEach(AsTemplate::convertToTemplate);
        return relations;
    }


}