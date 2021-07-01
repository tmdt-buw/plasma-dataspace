package de.buw.tmdt.plasma.ars.modeling.sm.service.rest.controller;

import de.buw.tmdt.plasma.ars.modeling.sm.service.handler.StandardMatchingHandler;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.api.StandardMatchingAPI;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.datamodel.CombinedModel;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sharedconfigs.ConfigService;

import java.util.List;
import java.util.Map;

@RestController("Standard Matching Controller")
public class StandardMatchingController implements StandardMatchingAPI {

    private final StandardMatchingHandler standardMatchingHandler;
    private final ConfigService configService;

    @Autowired
    public StandardMatchingController(
            StandardMatchingHandler standardMatchingHandler,
            ConfigService configService) {
        this.standardMatchingHandler = standardMatchingHandler;
        this.configService = configService;
    }

    @Override
    @Operation(description = "persists a new standard")
    public String createStandard(@RequestBody StandardProvisionDTO standardProvisionDTO) {
        return standardMatchingHandler.createStandard(standardProvisionDTO);
    }

    @Override
    @Operation(description = "returns a list of all existing standards")
    public List<StandardDTO> getStandards() {
        return standardMatchingHandler.getStandards();
    }

    @Override
    @Operation(description = "returns the semantic model for the given id")
    public CombinedModel fetchResult(String dataSourceId) {
        return standardMatchingHandler.fetchModel(dataSourceId);
    }

    @Override
    @Operation(description = "performs semantic modeling using the standard defined by the config on the given combined model")
    public @NotNull CombinedModel performModeling(@NotNull String dataSourceUuid, String configId, @NotNull CombinedModel model) {
        Map<String, String> config = configService.getConfig(configId);
        String standardId = String.valueOf(config.get("STANDARD_ID"));
        if (standardId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid standard id has to be provided as the \"STANDARD_ID\" field. Was \"" + standardId + "\".");
        }
        standardMatchingHandler.modelWithStandard(model, standardId, dataSourceUuid);
        return model;
    }
}