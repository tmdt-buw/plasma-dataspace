package de.buw.tmdt.plasma.ars.modeling.sm.shared.api;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.sas.shared.dto.syntaxmodel.NodeDTO;
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
	 * Starts to model, using a standard and returns the id of the analysis used to fetch the results
	 *
	 * @param syntaxModel Syntax Model of the Data Source
	 * @param standardId  Id of the Standard to be applied
	 * @return id of the analysis
	 */
	@PostMapping(path = "/model", produces = MediaType.APPLICATION_JSON_VALUE)
	String modelWithStandard(@NotNull @RequestBody NodeDTO syntaxModel,
							 @RequestParam("standardId") String standardId,
							 @RequestParam("analysisId") String analysisId);


	/**
	 * Fetches a created semantic model
	 *
	 * @param analysisId id used to store the results
	 * @return Semantic Model including identified standards
	 */
	@GetMapping(path = "/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
	SemanticModelDTO fetchResult(@RequestParam("analysisId") String analysisId);

}
