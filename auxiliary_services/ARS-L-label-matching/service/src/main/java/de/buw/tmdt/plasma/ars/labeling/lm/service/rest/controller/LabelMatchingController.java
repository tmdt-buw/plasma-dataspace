package de.buw.tmdt.plasma.ars.labeling.lm.service.rest.controller;

import de.buw.tmdt.plasma.ars.labeling.lm.service.service.LabelMatchingService;
import de.buw.tmdt.plasma.ars.labeling.lm.service.rest.LabelMatchingAPI;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.modification.DeltaModification;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("Label Matching Controller")
public class LabelMatchingController implements LabelMatchingAPI {

	private final LabelMatchingService labelMatchingService;

	@Autowired
	public LabelMatchingController(
			@NotNull LabelMatchingService labelMatchingService
	) {

		this.labelMatchingService = labelMatchingService;
	}

	@NotNull
	@Override
	@Operation(description = "performs a semantic labeling on the given combined model")
	public List<DeltaModification> performLabeling(@NotNull String uuid, String configId, String configToken, @NotNull CombinedModel combined) {
		return labelMatchingService.performLabeling(uuid, configId, configToken, combined);
	}
}