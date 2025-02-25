package de.buw.tmdt.plasma.services.dps.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.services.dps.conversion.ConversionException;
import de.buw.tmdt.plasma.services.dps.conversion.rdf.RDFConverter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Service
public class DataProcessingHandler {

    private final Logger logger = LoggerFactory.getLogger(DataProcessingHandler.class);

    private final DataStorageHandler dataStorageHandler;

    @Value("nyi")
    private String kgsOntologyAPI;

    private WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(DataProcessingHandler.class);

    @Autowired
    public DataProcessingHandler(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                                 DataStorageHandler dataStorageHandler) {
        this.dataStorageHandler = dataStorageHandler;
    }

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl("/")
                .filter((request, next) -> {
                    log.info("Making request to: {}", request.url());
                    return next.exchange(request);
                })
                .build();
    }

    public String processFile(CombinedModel template, String dataId, String fileId) {
        List<File> files = dataStorageHandler.listFiles(dataId);
        File file;
        if (fileId == null) {
            file = files.get(files.size() - 1);
        } else {
            file = files.stream()
                    .filter(x -> fileId.equals(x.getName()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested fileId could not be found"));
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(file);
        } catch (IOException e) {
            log.info("Could not read contents of file {}", file.getName(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read file.");
        }
        List<JsonNode> nodes = new ArrayList<>();
        if (root.isArray()) {
            root.elements().forEachRemaining(nodes::add);
        } else {
            nodes.add(root);
        }

        try {
            HashMap<String, String> prefixes = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/prefixes")
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<HashMap<String, String>>() {})
                .onErrorResume(e -> Mono.empty())
                .block();

            if (prefixes == null) {
                prefixes = new HashMap<String, String>();
            }

            PrefixMapping prefixMapping = new PrefixMappingImpl();
            prefixMapping.setNsPrefixes(prefixes);
            RDFConverter converter = new RDFConverter(prefixMapping);
            Model combined = ModelFactory.createDefaultModel();
            combined.setNsPrefixes(prefixes);
            template.getSemanticModel().setId(UUID.randomUUID().toString());
            for (JsonNode node : nodes) {
                Model model = converter.convertToRDF(template, node);
                combined.add(model);
            }
            return asTurtle(combined);
        } catch (ConversionException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not convert file content(s): " + e.getMessage());
        } catch (ResponseStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not obtain prefixes from KGS.");
        }
    }

    public static String asTurtle(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TURTLE");
        return out.toString();
    }

}
