package de.buw.tmdt.plasma.ars.modeling.sm.shared.api;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "/api/plasma-ars-m-sm")
public interface StandardMatchingAPI {

	@PostMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	String createStandard(@RequestBody StandardProvisionDTO standardProvisionDTO);

	@GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	List<StandardDTO> getStandards();

	/**
	 * Fetches a created semantic model.
	 *
	 * @param dataSourceId id used to store the results
	 * @return Semantic Model including identified standards
	 */
	@GetMapping(path = "/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel fetchResult(@RequestParam("dataSourceId") String dataSourceId);

	/**
	 * Starts to model, using a standard and returns the id of the analysis used to fetch the results.
	 *
	 * @param model          CombinedModel containing {@link de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel} of the Data Source
	 * @param dataSourceUuid The uuid of the data source used to reference this analysis
	 * @param configId       The id of the config to retrieve
	 *                       The config must have the "STANDARD_ID" set
	 * @return Returns the original model. Use {@link #fetchResult(String)} to retrieve the result later on
	 */
	@NotNull
	@PostMapping(value = "model", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	CombinedModel performModeling(@NotNull @RequestParam("uuid") String dataSourceUuid,
								  @RequestParam("configId") String configId,
								  @NotNull @RequestBody CombinedModel model);

}
