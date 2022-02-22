package de.buw.tmdt.plasma.services.kgs.shared.api;

import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.Set;

@SuppressWarnings("HardcodedFileSeparator - API URLs are OK")
@RequestMapping("/api/plasma-kgs/semanticModels")
public interface SemanticModelApi {

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull SemanticModel createSemanticModel(@NotNull @RequestBody SemanticModel request);

    /**
     * Utility function to find a semantic model by its id.
     *
     * @param id The id to look for
     * @return An existing model. Will throw 404 if non found.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull SemanticModel getSemanticModel(@NotNull @PathVariable("id") String id);

    @DeleteMapping(value = "/{id}")
    void deleteSemanticModel(@NotNull @PathVariable("id") String id);

    /**
     * Converts a semantic model into a specified format.
     *
     * @param id     The id of the semantic model in the knowledge graph.
     * @param format The format of the output (Turtle or RDF/XML)
     * @param flat   A flag that indicates whether entity types should be mapped to entity concepts in the RDF output.
     *
     * @return A semantic model in RDF Turtle format
     */
    @GetMapping(value = "/{id}/export", produces = MediaType.TEXT_PLAIN_VALUE)
    @NotNull ResponseEntity<String> convertSemanticModel(
            @NotNull @PathVariable("id") String id,
            @NotNull @RequestParam(name = "format", defaultValue = "turtle") String format,
            @RequestParam(defaultValue = "false", name = "flat") boolean flat
    ) throws MalformedURLException;

    /* ################################ PREDEFINED QUERY FUNCTIONS ############################### */

    @GetMapping(value = "/byElement", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull Set<SemanticModel> searchByElementLabel(@NotNull @RequestParam(value = "searchTerm", defaultValue = "", required = false) String searchTerm);

    @GetMapping(value = "/bySynonym", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull Set<SemanticModel> searchBySynonym(@NotNull @RequestParam(value = "searchTerm", defaultValue = "", required = false) String searchTerm);

    @GetMapping(value = "/allByLabel", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull Set<SemanticModel> searchAllByLabel(@NotNull @RequestParam(value = "searchTerm", defaultValue = "", required = false) String searchTerm);

}