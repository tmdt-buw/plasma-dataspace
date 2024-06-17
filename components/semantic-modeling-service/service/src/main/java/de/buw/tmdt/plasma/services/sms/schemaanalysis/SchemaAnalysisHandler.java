package de.buw.tmdt.plasma.services.sms.schemaanalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.datamodel.modification.operation.DataType;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.concurrent.TimeUnit.*;

@Service
public class SchemaAnalysisHandler {

    private final Logger log = LoggerFactory.getLogger(SchemaAnalysisHandler.class);
    private static final int ANALYSIS_REMOVAL_TIME_H = 24;

    private HashMap<String, Analysis> analyses;

    private final StructureIdentificationService identificationService;

    @Autowired
    public SchemaAnalysisHandler(StructureIdentificationService identificationService) {
        this.identificationService = identificationService;
        analyses = new HashMap<>();
    }

    public void initAnalysis(String id) {
        Analysis analysis = new Analysis(id);
        analyses.put(id, analysis);
        log.info("Initialized Schema Analysis with ID: {}", id);
    }

    public Analysis getAnalysis(String id) {
        if (analyses.containsKey(id)) {
            return analyses.get(id);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested analysis does not exist: " + id);
        }
    }

    public void addDataPoint(String id, String sample) {
        Analysis analysis = getAnalysis(id);
        // logger.info("Adding Datapoint to Schema Analysis with UUID:{} : {}", uuid, schemaAnalysisDataProvisionDTO.getData());

        SyntaxModel result;
        try {
            JsonNode root = new ObjectMapper().readTree(sample);
            result = identificationService.convertRootNode(root);
        } catch (JsonProcessingException | SchemaAnalysisException e) {
            log.error("Could not create JSON from input data", e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not identify syntax model");
        }

        // logger.info("Identified Sub Schema: {}", result);
        analysis.addSubAnalysis(result);
    }

    public @NotNull SyntaxModel getResult(String id, int exampleLimit) {
        Analysis analysis = getAnalysis(id);
        SyntaxModel syntaxModel = analysis.getResult();
        if (syntaxModel == null) {
            throw new ResponseStatusException(HttpStatus.TOO_EARLY, "Schema analysis not yet finished");
        }
        syntaxModel.getNodes().stream()
                .filter(schemaNode -> schemaNode instanceof PrimitiveNode)
                .map(schemaNode -> (PrimitiveNode) schemaNode)
                .forEach(primitiveNode -> primitiveNode.setExamples(new ArrayList<>(primitiveNode.getExamples().subList(0, Math.min(exampleLimit, primitiveNode.getExamples().size())))));
        return syntaxModel;
    }

    public void finish(String id) {
        Analysis analysis = getAnalysis(id);
        SyntaxModel mergedSchema = null;

        if (analysis.getSampleModels().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No samples added yet.");
        }
        // merge all sample models into one
        // since collision handling has been removed, this results in just gathering example values
        int size = 0;
        for (SyntaxModel sampleModel : analysis.getSampleModels()) {
            if (mergedSchema == null) {
                mergedSchema = sampleModel;
                size = (int) mergedSchema.getNodes().stream().filter(node -> node instanceof PrimitiveNode).count();
            } else {
                mergedSchema.getNodes().stream().filter(node -> node instanceof PrimitiveNode)
                        .forEach(primitiveNode -> {
                            String path = primitiveNode.getPathAsJSONPointer();
                            Optional<PrimitiveNode> match = sampleModel.getNodes().stream()
                                    .filter(sample -> sample.getPathAsJSONPointer().equals(path))
                                    .map(node -> (PrimitiveNode) node)
                                    .findFirst();
                            if (match.isEmpty()) {
                                log.info("Missing primitive node for path {}", path);
                            } else {
                                ((PrimitiveNode) primitiveNode).getExamples().add(match.get().getExamples().get(0));
                            }
                        });
            }
        }
        assert (mergedSchema != null);
        // now determine the datatypes for each primitive node
        mergedSchema.getNodes().stream().filter(node -> node instanceof PrimitiveNode)
                .map(node -> (PrimitiveNode) node)
                .forEach(primitiveNode -> {
                    primitiveNode.setDataType(predictDataType(primitiveNode.getExamples()));
                });

        analysis.setResult(mergedSchema);
    }

    private DataType predictDataType(List<String> examples) {
        if (!examples.isEmpty()) {
            if (isBoolean(examples)) {
                return DataType.Boolean;
            } else if (isNumber(examples)) {
                return DataType.Number;
            } else {
                return DataType.String;
            }
        } else {
            return DataType.Unknown;
        }
    }

    private boolean isBoolean(List<String> examples) {
        String[] refs = {"true", "false", "1", "0"};
        return examples.stream().allMatch(value -> value != null &&
                Arrays.stream(refs).anyMatch(b -> b.equalsIgnoreCase(value)));
    }

    private boolean isNumber(List<String> examples) {
        return examples.stream().allMatch(value -> {
            try {
                Double r = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        });
    }

    public boolean hasResult(String id) {
        Analysis analysis = getAnalysis(id);
        return analysis.getResult() != null;
    }

    public void delete(String id) {
        analyses.remove(id);
    }

    public boolean exists(String id) {
        return analyses.containsKey(id);
    }

    @Scheduled(cron="0 0 1 * * * ")
    public void cleanUpAnalyses() {
        LocalDateTime now = LocalDateTime.now();
        int before = analyses.size();
        analyses.entrySet().removeIf(entry ->
           Duration.between(entry.getValue().getCreation().toInstant(), now).toHours() > ANALYSIS_REMOVAL_TIME_H
        );
        log.info("Cleaned {} stale analyses.", before - analyses.size());
    }
}
