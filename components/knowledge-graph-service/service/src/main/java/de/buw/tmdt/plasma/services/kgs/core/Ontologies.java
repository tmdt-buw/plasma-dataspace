package de.buw.tmdt.plasma.services.kgs.core;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;

public class Ontologies {

    static String SM_uri = "http://plasma.uni-wuppertal.de/sm/";
    static String PLASMA_uri = "http://plasma.uni-wuppertal.de/ontology#";

    static PrefixMapping PREFIXES = new PrefixMappingImpl()
            .setNsPrefixes(PrefixMapping.Standard)
            .setNsPrefix("plcm", PLCM.getURI())
            .setNsPrefix("plsm", SM_uri)
            .setNsPrefix("plasma", PLASMA_uri);
}
