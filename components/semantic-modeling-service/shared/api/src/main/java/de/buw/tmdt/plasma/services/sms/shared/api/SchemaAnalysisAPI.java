package de.buw.tmdt.plasma.services.sms.shared.api;

import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides endpoints for schema analysis operations.
 * Expected usage pattern:
 * - initAnalysis
 * - addDataPoint (multiple times)
 * - finish
 * - ready (until it returns true)
 * - getResult
 * - delete
 */
@RequestMapping("/api/sms/analysis")
@SuppressWarnings("HardcodedFileSeparator - uris")
public interface SchemaAnalysisAPI {

	@PostMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	void initAnalysis(@PathVariable("id") String id);

	@PostMapping(path = "/{id}/samples", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	void addSample(@PathVariable("id") String id, @RequestBody String sample);

	@PostMapping(path = "/{id}/samples/multi", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	void addSamples(@PathVariable("id") String id, @RequestBody List<String> samples);

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@NotNull SyntaxModel getResult(@PathVariable("id") String id, @RequestParam(value = "exampleLimit", defaultValue = "100") int exampleLimit);

	@GetMapping(path = "/{id}/ready", produces = MediaType.APPLICATION_JSON_VALUE)
	boolean isReady(@PathVariable("id") String id);

	@PostMapping(path = "/{id}/finish", produces = MediaType.APPLICATION_JSON_VALUE)
	void finish(@PathVariable("id") String id);

	@GetMapping(path = "/{id}/existing", produces = MediaType.APPLICATION_JSON_VALUE)
	boolean existing(@PathVariable("id") String id);

	@DeleteMapping(path = "/{id}")
	void delete(@PathVariable("id") String id);
}
