package de.buw.tmdt.plasma.ars.labeling.lm.service.converter;


import de.buw.tmdt.plasma.services.srs.api.semanticmodel.EntityConceptDTO;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class KGStoSRSDTOConverter {

    public static EntityConceptDTO toSRS(de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO kgs){
        return new EntityConceptDTO(kgs.getId(),kgs.getMainLabel(),kgs.getDescription(),kgs.getSourceURI());
    }

    public static Collection<EntityConceptDTO> toSRS(Collection<de.buw.tmdt.plasma.services.kgs.shared.dto.knowledgegraph.EntityConceptDTO> kgs){
        return kgs.stream().map(KGStoSRSDTOConverter::toSRS).collect(Collectors.toSet());
    }
}
