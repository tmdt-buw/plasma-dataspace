package de.buw.tmdt.plasma.services.srs.api;

import de.buw.tmdt.plasma.services.srs.api.syntaxmodel.SyntaxModelDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "api/srs")
public interface SemanticRecommendationAPI {

	//############################################### Initial requests ###############################################

	/**
	 * Requests a semantic labeling for the syntactic model.
	 * @param syntaxModelDTO The syntax model
	 * @param uuid A reference uuid to identify the modeling process which may also be used to request metadata.
	 *             Usually the data source id
	 * @return A combined model containing a new semantic labeling as a basic semantic model
	 */
	@PostMapping(value = "/labeling",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performInitialSemanticLabeling( @RequestParam("uuid") String uuid, @NotNull @RequestBody SyntaxModelDTO syntaxModelDTO);

	/**
	 * Requests a semantic modeling for the combined model.
	 * @param dataSourceSchemaDTO The current state of the model. Semantic model may be empty.
	 * @param uuid A reference uuid to identify the modeling process which may also be used to request metadata.
	 *             Usually the data source id
	 * @return The combined model
	 */
	@NotNull
	@PostMapping(value = "/modeling",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performInitialSemanticModeling(@RequestParam("uuid") String uuid,@NotNull @RequestBody DataSourceSchemaDTO dataSourceSchemaDTO);

	/**
	 * Requests a semantic refinement for the combined model.
	 * @param dataSourceSchemaDTO The current state of the model. Semantic model may be empty.
	 * @param uuid A reference uuid to identify the modeling process which may also be used to request metadata.
	 *             Usually the data source id
	 * @return The combined model containing the recommended changes
	 */
	@PostMapping(value = "/refinement", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performInitialSemanticRefinement(@RequestParam("uuid") String uuid,@NotNull @RequestBody DataSourceSchemaDTO dataSourceSchemaDTO);

	//############################################### Follow-up requests #############################################

	/**
	 * Requests a semantic modeling for the combined model.
	 * @param dataSourceSchemaDTO The current state of the model. Semantic model may be empty.
	 * @param uuid A reference uuid to identify the modeling process which may also be used to request metadata.
	 *             Usually the data source id
	 * @return The combined model
	 */
	@NotNull
	@PostMapping(value = "/modeling/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performSemanticModeling(@NotNull @PathVariable("uuid") String uuid,@NotNull @RequestBody DataSourceSchemaDTO dataSourceSchemaDTO);

	/**
	 * Requests a semantic refinement for the combined model.
	 * @param dataSourceSchemaDTO The current state of the model. Semantic model may be empty.
	 * @param uuid A reference uuid to identify the modeling process which may also be used to request metadata.
	 *             Usually the data source id
	 * @return The combined model containing the recommended changes
	 */
	@PostMapping(value = "/refinement/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performSemanticRefinement(@NotNull @PathVariable("uuid") String uuid, @NotNull @RequestBody DataSourceSchemaDTO dataSourceSchemaDTO);


}
