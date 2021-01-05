package de.buw.tmdt.plasma.ars.modeling.sm.shared.api;

import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.syntaxmodel.NodeDTO;
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

	@PostMapping(path = "/model", produces = MediaType.APPLICATION_JSON_VALUE)
	SemanticModelDTO modelWithStandard(@NotNull @RequestBody NodeDTO syntaxModel, @RequestParam("standardId") String standardId);

}
