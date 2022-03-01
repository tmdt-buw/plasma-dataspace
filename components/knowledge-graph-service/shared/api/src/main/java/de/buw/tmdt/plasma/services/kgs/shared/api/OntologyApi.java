package de.buw.tmdt.plasma.services.kgs.shared.api;

import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModelNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping("/api/plasma-kgs/ontology")
public interface OntologyApi {

    /**
     * Find {@link SemanticModelNode}s filtered by a given prefix, infix or uri.
     * If a URI is specified, this takes precedence over the prefix and infix, thus, only an exact matching concept is returned or an empty list.
     *
     * @param prefix The prefix to search for, can be a partial match but is only evaluated at the start of the label
     * @param infix  The infix/substring to search for
     * @param uri    The uri to match. Has to be an exact match to the specified URI of the concept.
     * @return A list of matching {@link SemanticModelNode}s
     */
    @GetMapping(value = "/nodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull List<SemanticModelNode> getElements(
            @RequestParam(value = "prefix", defaultValue = "", required = false) String prefix,
            @RequestParam(value = "infix", defaultValue = "", required = false) String infix,
            @RequestParam(value = "uri", defaultValue = "", required = false) String uri);

    /**
     * Find {@link Relation}s filtered by a given prefix, infix or uri.
     * If a URI is specified, this takes precedence over the prefix and infix, thus, only an exact matching concept is returned or an empty list.
     *
     * @param prefix The prefix to search for, can be a partial match but is only evaluated at the start of the label
     * @param infix  The infix/substring to search for
     * @param uri    The uri to match. Has to be an exact match to the specified URI of the concept
     * @return A list of matching {@link Relation}s
     */
    @GetMapping(value = "/relations", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull List<Relation> getRelations(
            @RequestParam(value = "prefix", defaultValue = "", required = false) String prefix,
            @RequestParam(value = "infix", defaultValue = "", required = false) String infix,
            @RequestParam(value = "uri", defaultValue = "", required = false) String uri);

}