package de.buw.tmdt.plasma.ars.labeling.lm.service.rest.controller;

import de.buw.tmdt.plasma.ars.labeling.lm.service.handler.LabelMatchingHandler;
import de.buw.tmdt.plasma.ars.labeling.lm.shared.api.LabelMatchingAPI;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sharedconfigs.ConfigService;

@RestController("Label Matching Controller")
@RequestMapping(value = "/api/plasma-ars-l-lm")
public class LabelMatchingController implements LabelMatchingAPI {

	private final LabelMatchingHandler labelMatchingHandler;

	private ConfigService configClient;

	@Autowired
	public LabelMatchingController(
			@NotNull LabelMatchingHandler labelMatchingHandler,
			@NotNull ConfigService configService
	) {

		this.labelMatchingHandler = labelMatchingHandler;
		this.configClient = configService;
	}

	@NotNull
	@Override
	@Operation(description = "performs a semantic labeling on the given combined model")
	public CombinedModel performLabeling(@NotNull String uuid, String configId, @NotNull CombinedModel combined) {
		return labelMatchingHandler.performLabeling(uuid, configId, combined);
	}
}