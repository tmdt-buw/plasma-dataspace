package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class SemanticRefinementHandler {
    @NotNull
    public CombinedModel performRefinement(@NotNull String uuid, String configId, String configToken, @NotNull CombinedModel combinedModel) {
        // call ARS-R here

        // TODO ensure that all recommendations have a non-empty set of anchor points
        return combinedModel;
    }

}
