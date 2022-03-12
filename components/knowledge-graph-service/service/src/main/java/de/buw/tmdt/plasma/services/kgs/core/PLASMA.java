package de.buw.tmdt.plasma.services.kgs.core;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

// TODO remove once file based ontologies are supported
public class PLASMA{

    public static final String uri = "http://plasma.uni-wuppertal.de/ontology#";

    public static final Resource NamedEntity = Init.namedEntity();

    protected static final Resource resource(String local) {
        return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    /**
     * returns the URI for this schema
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    public static class Init {
        public static Resource namedEntity() {
            return resource("NamedEntity");
        }
    }
}
