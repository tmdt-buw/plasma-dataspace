package de.buw.tmdt.plasma.ars.modeling.sm.service.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.buw.tmdt.plasma.ars.modeling.sm.service.converter.RuleDTOConverter;
import de.buw.tmdt.plasma.ars.modeling.sm.service.model.Standard;
import de.buw.tmdt.plasma.ars.modeling.sm.service.repository.StandardRepository;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StandardPopulator {

	private final StandardRepository standardRepository;
	private final RuleDTOConverter ruleDTOConverter;
	private static final Logger logger = LoggerFactory.getLogger(StandardPopulator.class);

	@SuppressWarnings("HardcodedFileSeparator - OK since it is a resource file")
	private static final String GEO_STANDARD_FILE = "standards/georules.json";
	@SuppressWarnings("HardcodedFileSeparator - OK since it is a resource file")
	private static final String GEO_STANDARD_NAME = "GEOSTANDARD";

	@Autowired
	public StandardPopulator(
			StandardRepository standardRepository,
			RuleDTOConverter ruleDTOConverter
	) {
		this.standardRepository = standardRepository;
		this.ruleDTOConverter = ruleDTOConverter;
	}

	@PostConstruct
	public void initializeData() {

		if (standardRepository.findAllByName(GEO_STANDARD_NAME).size() > 0) {
			logger.info("Geo Standard already available.");
			return;
		}

		logger.info("Initializing Geo Standard");
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			StandardProvisionDTO standardProvisionDTO = objectMapper.readValue(getResourceReader(GEO_STANDARD_FILE), StandardProvisionDTO.class);
			Standard standard = new Standard(
					standardProvisionDTO.getName(),
					standardProvisionDTO.getDescription(),
					standardProvisionDTO.getEntityRules().stream().map(ruleDTOConverter::fromDTO).collect(Collectors.toList()),
					standardProvisionDTO.getRelationRules().stream().map(ruleDTOConverter::fromDTO).collect(Collectors.toList())
			);

			this.standardRepository.save(standard);

		} catch (IOException e) {
			logger.error("Could not initialize Geo Standard", e);
		}
	}

	@NotNull
	private InputStreamReader getResourceReader(@NotNull String resourceName) {
		return new InputStreamReader(
				Objects.requireNonNull(StandardPopulator.class.getClassLoader().getResourceAsStream(resourceName)),
				StandardCharsets.UTF_8
		);
	}

}
