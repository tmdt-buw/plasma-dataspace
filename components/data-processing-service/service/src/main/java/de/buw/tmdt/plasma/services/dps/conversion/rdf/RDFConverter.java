package de.buw.tmdt.plasma.services.dps.conversion.rdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Literal;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

public class RDFConverter {

    public static final Logger log = LoggerFactory.getLogger(RDFConverter.class);

    private final PrefixMapping prefixes;
    int counter = 0;


    /**
     * Initialize a new {@link RDFConverter}.
     */
    public RDFConverter(PrefixMapping prefixes) {
        this.prefixes = prefixes;
    }

    private int getCounter(){
        return counter++;
    }

    public Model convertToRDF(SemanticModel semanticModel) {
        counter = 0;
        Map<String, RDFNode> uuidtoResource = new HashMap<>();
        String semanticModelId = semanticModel.getId().substring(0, 5);
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(prefixes);
        String smPrefix = prefixes.getNsPrefixURI("plsm");

        semanticModel.getNodes().forEach(node -> {
            RDFNode rdfNode = null;
            if (node instanceof Class) {
                Class clazz = (Class) node;
                Instance i = clazz.getInstance();
                Resource resource;
                if (i != null) {
                    resource =
                            m.createResource(smPrefix + semanticModelId + "-" + convertToInstanceId(i.getLabel())); // will reuse if already existent
                    resource.addProperty(PLCM.label, m.createLiteral(i.getLabel()));
                    if (i.getDescription() != null && !i.getDescription().isBlank()) {
                        resource.addProperty(PLCM.description, m.createLiteral(i.getDescription()));
                    }
                } else {
                    resource = m.createResource(smPrefix+ semanticModelId + "-"  + getCounter());
                }

                resource.addProperty(RDF.type, m.createResource(m.expandPrefix(clazz.getURI())));
                rdfNode = resource;
            } else if (node instanceof Literal) {
                Literal l = (Literal) node;
                rdfNode = m.createLiteral(l.getValue());
            } else if (node instanceof NamedEntity) {
                NamedEntity ne = (NamedEntity) node;
                // create a resource which describes the named entity
                // this will later be elevated to the ontology
                Resource neResource = m.createResource(m.expandPrefix(ne.getURI()));
                neResource.addProperty(RDF.type, PLCM.NamedEntity);

                Resource resource = m.createResource();
                resource.addProperty(RDF.type, neResource);
                // use the b-node as reference
                rdfNode = resource;
            }
            uuidtoResource.put(node.getUuid(), rdfNode);

        });
        semanticModel.getEdges().forEach(r -> {
            SemanticModelNode from = semanticModel.getNode(r.getFrom());
            SemanticModelNode to = semanticModel.getNode(r.getTo());
            RDFNode fromNode = uuidtoResource.get(from.getUuid());
            RDFNode toNode = uuidtoResource.get(to.getUuid());
            fromNode.asResource().addProperty(ResourceFactory.createProperty(m.expandPrefix(r.getURI())), toNode);
        });
        return m;
    }


    public static String convertToInstanceId(String label) {
        String formatted = label.trim().toLowerCase().replace(" ", "_");
        formatted = HtmlUtils.htmlEscape(formatted);
        formatted = formatted.replaceAll("\\[", "_");
        formatted = formatted.replaceAll("\\]", "_");
        formatted = formatted.replaceAll("\\)", "_");
        formatted = formatted.replaceAll("\\(", "_");
        return formatted;
    }

}
