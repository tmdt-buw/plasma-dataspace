package de.buw.tmdt.plasma.services.dps.rest.controller;

import de.buw.tmdt.plasma.services.dps.api.DataProcessingApi;
import de.buw.tmdt.plasma.services.dps.api.SampleDTO;
import de.buw.tmdt.plasma.services.dps.core.DataProcessingHandler;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController("Data Processing Controller")
public class DataProcessingController implements DataProcessingApi {

	private final DataProcessingHandler dataProcessingHandler;


	public DataProcessingController(DataProcessingHandler dataProcessingHandler) {
		this.dataProcessingHandler = dataProcessingHandler;
	}

	@Override
	@Operation(description = "Initializes a new modeling.")
	public SampleDTO uploadFile(@Nullable String dataId, @NotNull MultipartFile file) {
		return dataProcessingHandler.uploadFile(dataId, file);
	}

	@Override
	@Operation(description = "List all files of an dataId.")
	public List<String> listFiles(@NotNull String dataId) {
		return dataProcessingHandler.listFiles(dataId);
	}

	@Override
	@Operation(description = "Check if service available.")
	public ResponseEntity<String> isAvailable() {
		return new ResponseEntity<>("OK", HttpStatus.OK);
	}
}
