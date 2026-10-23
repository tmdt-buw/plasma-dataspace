package de.buw.tmdt.plasma.services.sms.kg;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static de.buw.tmdt.plasma.services.sms.kg.OntologyManagement.PREFIXES;
import static de.buw.tmdt.plasma.services.sms.kg.SemanticModelService.SEMANTIC_MODEL_PREFIX_LENGTH;

@Service
public class SemanticModelManagement {

    private static final Logger log = LoggerFactory.getLogger(SemanticModelManagement.class);

    private RDFConnectionRemoteBuilder builder;

    public SemanticModelManagement(@Value("${plasma.kgs.semanticmodels.store.url}") String semanticModelServerAddress) {
        builder = RDFConnectionFuseki.create()
                .destination(semanticModelServerAddress);
    }

    public Model queryModel(String modelId) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build();
             QueryExecution description = conn.query(scopedQuery("DESCRIBE ?s ?p ?o", modelId, false));
             QueryExecution metadata = conn.query(scopedQuery("CONSTRUCT { ?s ?p ?o }", modelId, true))) {
            // DESCRIBE omits RDF-star annotations, which carry literal IDs and array contexts.
            return description.execDescribe().add(metadata.execConstruct());
        }
    }

    private static String scopedQuery(String operation, String modelId, boolean annotationsOnly) {
        if (modelId == null || modelId.isBlank()) throw new IllegalArgumentException("Model ID is required");
        String prefix = OntologyManagement.SM_URI + modelId.substring(0, Math.min(modelId.length(), SEMANTIC_MODEL_PREFIX_LENGTH));
        var query = new ParameterizedSparqlString(operation + " WHERE { ?s ?p ?o . "
                + (annotationsOnly ? "FILTER(isTRIPLE(?s)) " : "")
                + "BIND(IF(isTRIPLE(?s), SUBJECT(?s), ?s) AS ?owner) "
                + "FILTER(STR(?owner) = ?modelUri || STRSTARTS(STR(?owner), CONCAT(?modelUri, \"-\"))) }");
        query.setLiteral("modelUri", prefix);
        return query.toString();
    }

    public void persistModel(Model model) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            UpdateBuilder updateBuilder = new UpdateBuilder()
                    .addPrefixes(PREFIXES)
                    .addInsert(model);
            Update insert = updateBuilder.build();

            conn.update(insert);
        }
    }

    public void deleteModel(String semanticModelUuid) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            conn.update(scopedQuery("DELETE { ?s ?p ?o }", semanticModelUuid, false));
        }
    }

    public List<String> findByElementLabel(String searchTerm) {
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            SelectBuilder selectBuilder = new SelectBuilder();
            Query selector = selectBuilder
                    .addPrefixes(PREFIXES)
                    .addVar("?id")
                    .addWhere("?model", PLCM.hasNode, "?node")
                    .addWhere("?model", PLCM.cmUuid, "?id")
                    .addFilter(selectBuilder.getExprFactory().regex("?o", searchTerm, "i"))
                    .build();


            QueryExecution query = conn.query(selector);

            ResultSet resultSet = query.execSelect();
            List<String> ids = new ArrayList<>();
            while (resultSet.hasNext()) {
                QuerySolution querySolution = resultSet.next();
                Literal id = querySolution.getLiteral("id");
                ids.add(id.getString());
            }
            return ids;
        }
    }

}
