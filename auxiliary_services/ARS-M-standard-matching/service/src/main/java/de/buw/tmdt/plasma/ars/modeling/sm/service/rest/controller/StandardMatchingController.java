package de.buw.tmdt.plasma.ars.modeling.sm.service.rest.controller;

import de.buw.tmdt.plasma.ars.modeling.sm.service.handler.StandardMatchingHandler;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.api.StandardMatchingAPI;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.StandardProvisionDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.ars.modeling.sm.shared.dto.syntaxmodel.NodeDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("Standard Matching Controller")
public class StandardMatchingController implements StandardMatchingAPI {

    private final StandardMatchingHandler standardMatchingHandler;

    @Autowired
    public StandardMatchingController(
            StandardMatchingHandler standardMatchingHandler
    ) {
        this.standardMatchingHandler = standardMatchingHandler;
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

    @NotNull
    @Override
    @Operation(description = "performs semantic modeling using the defined standard on the given combined model")
    public SemanticModelDTO modelWithStandard(@NotNull NodeDTO syntaxModel, String standardId) {
        return standardMatchingHandler.modelWithStandard(syntaxModel, standardId);
    }


}