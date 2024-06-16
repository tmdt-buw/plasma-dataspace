package de.buw.tmdt.plasma.services.sms.schemaanalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.PrimitiveNode;
import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.converter.CombinedModelConverter;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.Analysis;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.syntaxmodel.Node;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class SchemaAnalysisHandler {


	private HashMap<String, Analysis> analyses;

	private final StructureIdentificationService identificationService;

	private final CombinedModelConverter combinedModelConverter;

	private final Logger logger = LoggerFactory.getLogger(SchemaAnalysisHandler.class);

	@Autowired
	public SchemaAnalysisHandler(StructureIdentificationService identificationService,
                                 CombinedModelConverter combinedModelConverter) {
        this.identificationService = identificationService;
        analyses = new HashMap<>();
		this.combinedModelConverter = combinedModelConverter;
	}

	public void initAnalysis(String id) {
		Analysis analysis = new Analysis(id);
		analyses.put(id,analysis);
		logger.info("Initialized Schema Analysis with ID: {}", id);
	}

	public Analysis getAnalysis(String id) {
		if(analyses.containsKey(id)) {
			return analyses.get(id);
		}
		else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested analysis does not exist: " + id);
		}
	}

	public void addDataPoint(String id, String sample) {
		Analysis analysis = getAnalysis(id);
		// logger.info("Adding Datapoint to Schema Analysis with UUID:{} : {}", uuid, schemaAnalysisDataProvisionDTO.getData());

		Node result;
		try {
			result = identificationService.execute(sample);
		} catch (JsonProcessingException | SchemaAnalysisException e) {
			logger.error("Could not create JSON from input data", e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not identify syntax model");
		}

		// logger.info("Identified Sub Schema: {}", result);
		analysis.addSubAnalysis(result);
	}

	public @NotNull SyntaxModel getResult(String id, int exampleLimit) {
		Analysis analysis = getAnalysis(id);
		Node result = analysis.getResult();
		if(result == null) {
			throw new ResponseStatusException(HttpStatus.TOO_EARLY, "Schema analysis not yet finished");
		}
		// TODO change datamodel
		SyntaxModel syntaxModel = combinedModelConverter.toCombinedModel(result);

		syntaxModel.getNodes().stream()
				.filter(schemaNode -> schemaNode instanceof PrimitiveNode)
				.map(schemaNode -> (PrimitiveNode) schemaNode)
				.forEach(primitiveNode -> primitiveNode.setExamples(new ArrayList<>(primitiveNode.getExamples().subList(0, Math.min(exampleLimit, primitiveNode.getExamples().size())))));
		return syntaxModel;
	}

	public void finish(String id) {
		Analysis analysis = getAnalysis(id);
		Node mergedSchema = null;

		for(Node subNode : analysis.getSubAnalyses()){

			if (mergedSchema == null) {
				mergedSchema = subNode;
			} else {
				mergedSchema = mergedSchema.merge(subNode);
			}
		}
		analysis.setResult(mergedSchema);
	}

	public boolean hasResult(String id) {
		Analysis analysis = getAnalysis(id);
		Node result = analysis.getResult();
		return result != null;
	}

	public void delete(String id) {
		analyses.remove(id);
	}

	public boolean exists(String id) {
		return analyses.containsKey(id);
	}
}
