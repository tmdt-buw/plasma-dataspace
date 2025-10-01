package de.buw.tmdt.plasma.services.sms.rest.controller;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.services.sms.kg.SemanticModelService;
import de.buw.tmdt.plasma.services.sms.shared.api.SemanticModelApi;
import de.buw.tmdt.plasma.services.sms.shared.model.ExportFormat;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class SemanticModelController implements SemanticModelApi {

    private final SemanticModelService semanticModelService;

    @Autowired
    public SemanticModelController(@NotNull SemanticModelService semanticModelService) {
        this.semanticModelService = semanticModelService;
    }

    @Override
    @Operation(description = "Load Semantic Model")
    public @NotNull SemanticModel getSemanticModel(@NotNull String id) {
        return semanticModelService.getSemanticModel(id);
    }

    @Override
    @Operation(description = "Deletes a semantic model")
    public void deleteSemanticModel(@NotNull String id) {
        semanticModelService.deleteSemanticModel(id);
    }

    @Operation(description = "Returns a stored semantic model in a specified format.")
    public @NotNull ResponseEntity<String> exportStoredSemanticModel(@NotNull String id, @NotNull ExportFormat format, boolean includeMappings, boolean includePositions) {
        return semanticModelService.exportSemanticModel(id, format, includeMappings, includePositions);
    }

    @Operation(description = "Returns the given semantic model in a specified format.")
    public @NotNull ResponseEntity<String> convertSemanticModel(@NotNull CombinedModel combinedModel, @NotNull ExportFormat format, boolean includeMappings, boolean includePositions) {
        return semanticModelService.convertSemanticModel(combinedModel, format, includeMappings, includePositions);
    }

    @Override
    @Operation(description = "Returns Semantic Models if the search term matches a contained element")
    public @NotNull Set<SemanticModel> searchByElementLabel(@NotNull String searchTerm) {
        return semanticModelService.searchByElementLabel(searchTerm);
    }

}
