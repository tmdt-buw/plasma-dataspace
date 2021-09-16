package de.buw.tmdt.plasma.services.kgs.shared.api;

import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptRelationDTO;
import de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.RelationConceptDTO;
import de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.EntityConcept;
import de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.RelationConcept;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping("/api/plasma-kgs/universalknowledge")
public interface UniversalKnowledgeApi {

    /* ********************************************** ENTITY CONCEPT **************************************************/

    @PostMapping(value = "/entityConcepts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull EntityConceptDTO createEntityConcept(@NotNull @RequestBody EntityConceptDTO request);

    /**
     * Find {@link de.buw.tmdt.plasma.services.kgs.shared.model.knowledgegraph.EntityConcept}s filtered by a given prefix or uri.
     * If a URI is specified, this takes precedence over the prefix, thus, only an exact matching concept is returned or an empty list.
     *
     * @param prefix The prefix to search for, can be a partial match but is only evaluated at the start of the {@link EntityConcept#getMainLabel()}
     * @param uri    The uri to match. Has to be an exact match to the specified URI of the concept.
     * @return A list of matching {@link EntityConcept}s in DTO form
     */
    @GetMapping(value = "/entityConcepts", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull List<EntityConceptDTO> getEntityConcepts(
            @RequestParam(value = "prefix", defaultValue = "", required = false) String prefix,
            @RequestParam(value = "uri", defaultValue = "", required = false) String uri);

    @DeleteMapping(value = "/entityConcepts/{id}")
    boolean deleteEntityConcept(@NotNull @PathVariable("id") String id);

    @GetMapping(value = "/entityConcepts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull EntityConceptDTO getEntityConceptById(@NotNull @PathVariable(value = "id") String id);

    /* ********************************************** RELATION CONCEPT **************************************************/

    @PostMapping(value = "/relationconcepts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull RelationConceptDTO createRelationConcept(@NotNull @RequestBody RelationConceptDTO request);

    /**
     * Find {@link RelationConcept}s filtered by a given prefix or uri.
     * If a URI is specified, this takes precedence over the prefix, thus, only an exact matching concept is returned or an empty list.
     *
     * @param prefix The prefix to search for, can be a partial match but is only evaluated at the start of the {@link RelationConcept#getLabel()}
     * @param uri    The uri to match. Has to be an exact match to the specified URI of the concept
     * @return A list of matching {@link RelationConcept}s in DTO form
     */
    @GetMapping(value = "/relationconcepts", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull List<RelationConceptDTO> getRelationConcepts(
            @RequestParam(value = "prefix", defaultValue = "", required = false) String prefix,
            @RequestParam(value = "uri", defaultValue = "", required = false) String uri);

    @DeleteMapping(value = "/relationConcepts/{id}")
    boolean deleteRelationConcept(@NotNull @PathVariable("id") String id);

    @GetMapping(value = "/relationConcepts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull RelationConceptDTO getRelationConceptById(@NotNull @PathVariable(value = "id") String id);

    /* ********************************************** ENTITY CONCEPT RELATION **************************************************/

    @PostMapping(value = "/entityconceptrelation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull EntityConceptRelationDTO createEntityConceptRelation(@NotNull @RequestBody EntityConceptRelationDTO request);

    @GetMapping(value = "/entityconceptrelation", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull List<EntityConceptRelationDTO>
    getEntityConceptRelations(@RequestParam(name = "relativeUsageCalculation", defaultValue = "true") boolean useRelativeCalculation);

    @DeleteMapping(value = "/deleteEntityConceptRelation/{id}")
    boolean deleteEntityConceptRelation(@NotNull @PathVariable("id") String id);

    @GetMapping(value = "/referenceCount/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    int getNumberOfReferences(@NotNull @PathVariable(value = "id") String id);

    /* ********************************************** RDF **************************************************/

    /**
     * Imports the rdf model into the knowledge graph by translating each triple into.
     * EC -> ECR(->RC) -> EC
     *
     * @param body The model in Turtle or RDF/XML format to be imported.
     *
     * @return A set of {@link EntityConceptRelationDTO} that correspond to the model triples.
     */
    @PostMapping(value = "/import", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotNull Set<EntityConceptRelationDTO> importRdf(@RequestBody String body);

    /**
     * Converts the underlying knowledge graph into a specified format.
     *
     * @param format The format of the output (Turtle or RDF/XML)
     * @param flat   A flag that indicates whether entity types should be mapped to entity concepts in the RDF output.
     *
     * @return The knowledge graph in RDF Turtle format.
     */
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    @NotNull String convertGraphToRdf(
            @NotNull @RequestParam(name = "format", defaultValue = "turtle") String format,
            @RequestParam(defaultValue = "false", name = "flat") boolean flat
    ) throws MalformedURLException;

    /**
     * Getter for the upper ontology of the KGS.
     *
     * @return A RDF Turtle formatted plain text that contains the upper ontology.
     */
    @GetMapping(value = "/upperOntology", produces = MediaType.TEXT_PLAIN_VALUE)
    @NotNull String getUpperOntology(@NotNull @RequestParam(name = "format", defaultValue = "turtle") String format) throws MalformedURLException;
}