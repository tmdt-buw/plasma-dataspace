package de.buw.tmdt.plasma.ars.refinement.recommendation.shared.api.api;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SuppressWarnings("HardcodedFileSeparator")
@RequestMapping(value = "/api/plasma-ars-r-rec")
public interface ARSRRecommendationAPI {

	@PostMapping(path = "/recommend", produces = MediaType.APPLICATION_JSON_VALUE)
	@NotNull CombinedModel recommend(@NotNull @RequestBody CombinedModel combinedModel);
}
