package de.buw.tmdt.plasma.ars.labeling.lm.service.rest.controller;

import de.buw.tmdt.plasma.ars.labeling.lm.service.handler.LabelMatchingHandler;
import de.buw.tmdt.plasma.ars.labeling.lm.shared.api.LabelMatchingAPI;
import de.buw.tmdt.plasma.services.srs.api.DataSourceSchemaDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController("Label Matching Controller")
public class LabelMatchingController implements LabelMatchingAPI {

	private final LabelMatchingHandler labelMatchingHandler;

	@Autowired
	public LabelMatchingController(
			@NotNull LabelMatchingHandler labelMatchingHandler
	) {

		this.labelMatchingHandler = labelMatchingHandler;
	}

	@NotNull
	@Override
	@Operation(description = "performs a semantic labeling on the given combined model")
	public DataSourceSchemaDTO performLabeling(@NotNull String uuid, @NotNull DataSourceSchemaDTO combined) {
		return labelMatchingHandler.performLabeling(uuid, combined);
	}
}