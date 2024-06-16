package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.SchemaAnalysisHandler;
import de.buw.tmdt.plasma.services.sms.shared.api.SchemaAnalysisAPI;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("Schema Analysis Controller")
public class SchemaAnalysisController implements SchemaAnalysisAPI {

	private final SchemaAnalysisHandler schemaAnalysisHandler;

	@Autowired
	public SchemaAnalysisController(SchemaAnalysisHandler schemaAnalysisHandler) {
		this.schemaAnalysisHandler = schemaAnalysisHandler;
	}

	@Override
	public void initAnalysis(String id) {
		schemaAnalysisHandler.initAnalysis(id);
	}

	@Override
	public void addSample(String id, String sample) {
		schemaAnalysisHandler.addDataPoint(id, sample);
	}

	@Override
	public void addSamples(String id, List<String> samples) {
		samples.forEach(sample -> schemaAnalysisHandler.addDataPoint(id, sample));
	}

	@Override
	public @NotNull SyntaxModel getResult(String id, int exampleLimit) {
		return schemaAnalysisHandler.getResult(id, exampleLimit);
	}

	@Override
	public boolean isReady(String id) {
		return schemaAnalysisHandler.hasResult(id);
	}

	@Override
	public void finish(String id) {
		schemaAnalysisHandler.finish(id);
	}

	@Override
	public boolean existing(String id) {
		return schemaAnalysisHandler.exists(id);
	}

	@Override
	public void delete(String id) {
		schemaAnalysisHandler.delete(id);
	}
}
