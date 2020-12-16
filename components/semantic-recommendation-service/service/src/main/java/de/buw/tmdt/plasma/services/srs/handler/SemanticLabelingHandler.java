package de.buw.tmdt.plasma.services.srs.handler;

import de.buw.tmdt.plasma.services.srs.api.DataSourceSchemaDTO;
import de.buw.tmdt.plasma.services.srs.api.recommendation.DeltaRecommendationDTO;
import de.buw.tmdt.plasma.services.srs.api.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.srs.api.syntaxmodel.SyntaxModelDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SemanticLabelingHandler {
    public DataSourceSchemaDTO performLabeling(String uuid, SyntaxModelDTO syntaxModelDTO) {

        // call ARS-L here

        List<DeltaRecommendationDTO> recommendations = new ArrayList<>();

        SemanticModelDTO semanticModelDTO = new SemanticModelDTO(null, null, "", "", new ArrayList<>(), new ArrayList<>());
        DataSourceSchemaDTO dataSourceSchemaDTO = new DataSourceSchemaDTO(syntaxModelDTO,semanticModelDTO,new ArrayList<>(),false);
        return dataSourceSchemaDTO;
    }
}
