package de.buw.tmdt.plasma.ars.labeling.lm.shared.api;

import de.buw.tmdt.plasma.services.srs.api.DataSourceSchemaDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "api/plasma-ars-l-lm")
public interface LabelMatchingAPI {

	@NotNull
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	DataSourceSchemaDTO performLabeling(@NotNull @RequestParam("uuid") String uuid, @NotNull @RequestBody DataSourceSchemaDTO combined);

}
