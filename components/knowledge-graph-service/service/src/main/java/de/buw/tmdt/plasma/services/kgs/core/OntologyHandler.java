package de.buw.tmdt.plasma.services.kgs.core;

import de.buw.tmdt.plasma.datamodel.semanticmodel.Relation;
import de.buw.tmdt.plasma.datamodel.semanticmodel.URIDefinedElement;
import de.buw.tmdt.plasma.services.kgs.core.rdf.KgsToRdfConverter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class OntologyHandler {

    private static final Logger log = LoggerFactory.getLogger(OntologyHandler.class);

    private static final String XML_VARIABLE = "xml";
    private static final String TURTLE_VARIABLE = "turtle";
    private final KgsToRdfConverter kgsToRdfConverter;

    private final OntologyManagement onto;

    @Autowired
    public OntologyHandler(
            @NotNull KgsToRdfConverter kgsToRdfConverter,
            @NotNull OntologyManagement onto) {
        this.kgsToRdfConverter = kgsToRdfConverter;
        this.onto = onto;
    }

    @NotNull
    public List<URIDefinedElement> getElements(@Nullable String prefix, @Nullable String infix, @Nullable String uri) {
        if (uri != null && !uri.isBlank()) {
            return Collections.singletonList(onto.findElementsByURI(uri));
        } else if (prefix != null && !prefix.isBlank()) {
            return onto.findElementsByLabelPrefix(prefix);
        } else if (infix != null && !infix.isBlank()) {
            return onto.findElementsByLabelInfix(infix);
        }
        return Collections.emptyList();
    }

    @NotNull
    public List<Relation> getRelations(@Nullable String prefix, @Nullable String infix, String uri) {
        if (uri != null && !uri.isBlank()) {
            return Collections.singletonList(onto.findRelationByURI(uri));
        } else if (prefix != null && !prefix.isBlank()) {
            return onto.findRelationsByLabelPrefix(prefix);
        } else if (infix != null && !infix.isBlank()) {
            return onto.findRelationsByLabelInfix(infix);
        }
        return Collections.emptyList();
    }

    /* ********************************************** RDF **************************************************/

    public @NotNull String convertGraphToRdf(@NotNull String format, boolean flat) throws MalformedURLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Model model = kgsToRdfConverter.convertKgsToRdf(flat);
        if (format.equals(XML_VARIABLE)) {
            RDFDataMgr.write(baos, model, Lang.RDFXML);
        } else if (format.equals(TURTLE_VARIABLE)) {
            RDFDataMgr.write(baos, model, Lang.TURTLE);
        } else {
            throw new MalformedURLException("Path variable \"format\" must either specify \"xml\" or \"format\" but states " + format);
        }

        return baos.toString(StandardCharsets.UTF_8);
    }
}