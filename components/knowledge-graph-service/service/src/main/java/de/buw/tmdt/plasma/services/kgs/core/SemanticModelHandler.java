package de.buw.tmdt.plasma.services.kgs.core;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import de.buw.tmdt.plasma.services.kgs.shared.model.ExportFormat;
import org.apache.jena.rdf.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SemanticModelHandler {

    private static final Logger log = LoggerFactory.getLogger(SemanticModelHandler.class);

    private static final String XML_VARIABLE = "xml";
    private static final String TURTLE_VARIABLE = "turtle";

    private final SemanticModelManagement ops;
    private final OntologyManagement onto;

    @Autowired
    public SemanticModelHandler(
            @NotNull SemanticModelManagement ops,
            @NotNull OntologyManagement onto) {
        this.ops = ops;
        this.onto = onto;
    }


    @NotNull
    public SemanticModel createSemanticModel(@NotNull SemanticModel semanticModel) {
        log.info("Creating Semantic Model");
        Model cmModel = CMToRDFMapper.convertToRDFModel(semanticModel);
        log.info(asTurtle(cmModel));

        ops.persistModel(cmModel);

        List<SemanticModelNode> resources = semanticModel.getNodes().stream()
                .filter(smn -> smn instanceof NamedEntity || smn instanceof Class)
                .collect(Collectors.toList());
        onto.addResourcesToOntology(resources);

        List<Relation> relations = semanticModel.getEdges();
        onto.addRelationsToOntology(relations);

        SemanticModel sm = getSemanticModel(semanticModel.getId());

        return sm;
    }

    @NotNull
    public SemanticModel getSemanticModel(@NotNull String id) {
        Model retrievedModel = ops.queryModel(id);
        log.info(asTurtle(retrievedModel));

        if(retrievedModel.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No model available for id "+ id);
        }
        SemanticModel restoredSM = CMToRDFMapper.convertToSemanticModel(retrievedModel, onto);
        return restoredSM;
    }

    public void deleteSemanticModel(@NotNull String id) {
        ops.deleteModel(id);
    }

    public void enhanceEntities(SemanticModel semanticModel) {

        semanticModel.getNodes().forEach(smn -> {
            if (smn instanceof Class) {
                Class semanticClass = (Class) smn;
                SemanticModelNode entity = onto.findElementsByURI(semanticClass.getURI());
                if (entity instanceof Class) {
                    semanticClass.setLabel(entity.getLabel());
                    semanticClass.setDescription(((Class) entity).getDescription());
                } else {
                    log.error(smn.getURI() + " is not a Class but is declared as one.");
                }
            } else if (smn instanceof NamedEntity) {
                SemanticModelNode entity = onto.findElementsByURI(smn.getURI());
                NamedEntity ne = (NamedEntity) smn;
                if (entity instanceof NamedEntity) {
                    NamedEntity enhanced = (NamedEntity) entity;
                    ne.setLabel(enhanced.getLabel());
                    ne.setDescription(enhanced.getDescription());
                } else {
                    log.error(smn.getURI() + " is not a NamedEntity but is declared as one.");
                }
            }
        });
    }

    public static String asTurtle(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TURTLE");
        return out.toString();
    }

    public @NotNull ResponseEntity<String> convertSemanticModel(@NotNull String id, @NotNull ExportFormat format) {
        Model retrievedModel = ops.queryModel(id);
        log.info(asTurtle(retrievedModel));

        if(retrievedModel.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No model available for id "+ id);
        }
        return new ResponseEntity<>(prepareForExport(retrievedModel, format), HttpStatus.OK);
    }

    public @NotNull ResponseEntity<String> convertSemanticModel(@NotNull CombinedModel combinedModel, @NotNull ExportFormat format) {
        if(combinedModel.getSemanticModel().getId() == null){
            combinedModel.getSemanticModel().setId(UUID.randomUUID().toString().substring(0,5));
        }
        Model cmModel = CMToRDFMapper.convertToRDFModel(combinedModel.getSemanticModel());
        log.info(asTurtle(cmModel));
        return new ResponseEntity<>(prepareForExport(cmModel, format), HttpStatus.OK);
    }

    private String prepareForExport(Model model, ExportFormat format){
        // remove all relations that are from the PLCM namespace
        Model cleansedModel = ModelFactory.createDefaultModel();
        cleansedModel.setNsPrefixes(model.getNsPrefixMap());
        StmtIterator stmtIterator = model.listStatements();
        while(stmtIterator.hasNext()){
            Statement stmt = stmtIterator.nextStatement();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
            if(predicate.getNameSpace().equals(PLCM.getURI())){
                continue;
            }
            if(object.isResource() && Objects.equals(object.asResource(),PLCM.SemanticModel)){
                continue;
            }
            cleansedModel.add(stmt);

        }
        StringWriter out = new StringWriter();
        cleansedModel.write(out, format.getFormatString());
        String result = out.toString();
        log.info(result);
       return result;
    }


    @NotNull
    public Set<SemanticModel> searchByEntityConcept(@NotNull String searchTerm) {
        List<String> ids = ops.findByElementLabel(searchTerm);
        return ids.stream()
                .map(this::getSemanticModel)
                .collect(Collectors.toSet());
    }
/*
    @NotNull
    public Set<SemanticModelDTO> searchByEntityType(@NotNull String searchTerm) {
        return graphDBDriverFactory.getGraphDBDriver().searchSemanticModelsForEntityType(searchTerm)
                .stream()
                .map(modelDTOConverter::mapSemanticModel)
                .collect(Collectors.toSet());
    }

    @NotNull
    public Set<SemanticModelDTO> searchBySynonym(@NotNull String searchTerm) {
        return graphDBDriverFactory.getGraphDBDriver().searchSemanticModelsForEntityConceptSynonymLabel(searchTerm)
                .stream()
                .map(modelDTOConverter::mapSemanticModel)
                .collect(Collectors.toSet());
    }

    @NotNull
    public Set<SemanticModelDTO> searchAllByLabel(@NotNull String searchTerm) {
        GraphDBDriver graphDBDriver = graphDBDriverFactory.getGraphDBDriver();
        Set<SemanticModel> allModels = new HashSet<>(graphDBDriver.searchSemanticModelsForEntityConceptAnyLabel(searchTerm));
        return allModels.stream().map(modelDTOConverter::mapSemanticModel).collect(Collectors.toSet());
    }

    @NotNull
    public Set<SemanticModelDTO> searchByEntityTypeOrEntityConceptContainingLabel(@NotNull String searchTerm) {
        return graphDBDriverFactory.getGraphDBDriver().searchSemanticModelByEntityTypeOrEntityConceptContainingLabel(searchTerm)
                .stream()
                .map(modelDTOConverter::mapSemanticModel)
                .collect(Collectors.toSet());
    }

     */
}