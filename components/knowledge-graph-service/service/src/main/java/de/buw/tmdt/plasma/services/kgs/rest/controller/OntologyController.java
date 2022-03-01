package de.buw.tmdt.plasma.services.kgs.rest.controller;

import de.buw.tmdt.plasma.datamodel.semanticmodel.NodeTemplate;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.RelationTemplate;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import de.buw.tmdt.plasma.services.kgs.core.OntologyHandler;
import de.buw.tmdt.plasma.services.kgs.shared.api.OntologyApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
public class OntologyController implements OntologyApi {

    private final OntologyHandler ontologyHandler;

    @Autowired
    public OntologyController(@NotNull OntologyHandler ontologyHandler) {
        this.ontologyHandler = ontologyHandler;
    }

    @Override
    public @NotNull List<SemanticModelNode> getElements(String prefix, String infix, String uri) {
        List<SemanticModelNode> elements = ontologyHandler.getElements(prefix, infix, uri);
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(element -> ((NodeTemplate) element).convertToTemplate());
        return elements;
    }

    @Override
    public @NotNull List<Relation> getRelations(String prefix, String infix, String uri) {
        List<Relation> relations = ontologyHandler.getRelations(prefix, infix, uri);
        relations.forEach(RelationTemplate::convertToTemplate);
        return relations;
    }


}