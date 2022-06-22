package de.buw.tmdt.plasma.services.dps.conversion.rdf;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.services.dps.CombinedModelGenerator;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static de.buw.tmdt.plasma.datamodel.CombinedModelGenerator.getCombinedModel;
import static de.buw.tmdt.plasma.services.dps.CombinedModelGenerator.asTurtle;
import static de.buw.tmdt.plasma.services.dps.CombinedModelGenerator.getFlightModel;

public class DataConverterTest {



    @Test
    public void testConversion() throws IOException, ConversionException {
        CombinedModel combinedModel = getFlightModel();

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = DataConverterTest.class.getResourceAsStream("/flight.json");
        JsonNode jsonNode = mapper.readTree(is);

        PrefixMapping prefixMapping = new PrefixMappingImpl();
        prefixMapping.setNsPrefix("local", CombinedModelGenerator.namespace);
        prefixMapping.setNsPrefix("plcm", PLCM.getURI());
        prefixMapping.setNsPrefix("plsm", "http://plasma.uni-wuppertal.de/sm/");

        RDFConverter converter = new RDFConverter(prefixMapping);

        DataInjector injector = new DataInjector();
        SemanticModel injected = injector.inject(combinedModel, jsonNode);

        Model converted = converter.convertToRDF(injected);
        String output = asTurtle(converted);

        System.out.println(output);
    }

    public Model cleanse(Model model){
        Model newModel = ModelFactory.createDefaultModel();
        newModel.setNsPrefixes(model.getNsPrefixMap());
        StmtIterator stmtIterator = model.listStatements();
        while(stmtIterator.hasNext()){
            Statement stmt = stmtIterator.nextStatement();
            if(stmt.getPredicate().getURI().contains("cmUuid")){
                continue;
            }
            if(stmt.getPredicate().getURI().contains("http://plasma.uni-wuppertal.de/cm#syntaxNodeId")){
                continue;
            }
            newModel.add(stmt);
        }
        return newModel;
    }




}
