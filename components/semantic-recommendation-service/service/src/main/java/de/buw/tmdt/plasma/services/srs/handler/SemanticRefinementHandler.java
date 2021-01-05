package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.springframework.stereotype.Service;

@Service
public class SemanticRefinementHandler {
    public CombinedModel performRefinement(String uuid, CombinedModel combinedModel) {
        // call ARS-R here

        // TODO ensure that all recommendations have a non-empty set of anchor points
        return combinedModel;
    }

}
