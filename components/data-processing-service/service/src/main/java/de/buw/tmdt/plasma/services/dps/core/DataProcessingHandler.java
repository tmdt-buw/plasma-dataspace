package de.buw.tmdt.plasma.services.dps.core;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import de.buw.tmdt.plasma.services.dps.conversion.rdf.DataInjector;
import de.buw.tmdt.plasma.services.dps.conversion.rdf.RDFConverter;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.OntologyApiClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataProcessingHandler {

    private final OntologyApiClient ontologyApiClient;

    @Autowired
    public DataProcessingHandler(OntologyApiClient ontologyApiClient) {
        this.ontologyApiClient = ontologyApiClient;
    }

    public void processFile(CombinedModel template, String dataId, String fileId){
        // TODO read file

        DataInjector injector = new DataInjector();

        List<SemanticModel> injected = null;
        try {
            injected = injector.inject(template, Collections.EMPTY_LIST);
            Map<String, String> prefixes = ontologyApiClient.getPrefixes();
            PrefixMapping prefixMapping = new PrefixMappingImpl();
            prefixMapping.setNsPrefixes(prefixes);
            RDFConverter converter = new RDFConverter(prefixMapping);
            List<Model> converted = injected.stream().map(converter::convertToRDF).collect(Collectors.toList());
        } catch (ConversionException e) {
            e.printStackTrace();
        }


    }
}
