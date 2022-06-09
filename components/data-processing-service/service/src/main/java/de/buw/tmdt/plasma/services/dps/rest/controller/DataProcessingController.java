package de.buw.tmdt.plasma.services.dps.rest.controller;

import de.buw.tmdt.plasma.services.dps.api.DataProcessingApi;
import de.buw.tmdt.plasma.services.dps.core.DataProcessingHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController("Data Processing Controller")
public class DataProcessingController implements DataProcessingApi {

	private final DataProcessingHandler schemaAnalysisHandler;


	public DataProcessingController(DataProcessingHandler schemaAnalysisHandler) {
		this.schemaAnalysisHandler = schemaAnalysisHandler;
	}
}
