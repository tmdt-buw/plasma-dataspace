package de.buw.tmdt.plasma.services.sms.kg;

import de.buw.tmdt.plasma.services.sms.modeling.ModelingService;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
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
        String semanticModelId = modelId.substring(0, SEMANTIC_MODEL_PREFIX_LENGTH);
        String prefix = OntologyManagement.SM_URI + semanticModelId;

        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            Query selector = new DescribeBuilder()
                    .addPrefixes(PREFIXES)
                    .addVar("?s")
                    .addVar("?p")
                    .addVar("?o")
                    .addWhere("?s", "?p", "?o")
                    .addFilter("STRSTARTS(STR(?s), \"" + prefix + "\")")
                    .build();

            QueryExecution query = conn.query(selector);
            Model model1 = query.execDescribe();
            return model1;
        } catch (ParseException e) {
            log.error("Cannot parse semantic model storage query expression", e);
            return null;
        }
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
        String semanticModelId = semanticModelUuid.substring(0, SEMANTIC_MODEL_PREFIX_LENGTH);
        String prefix = OntologyManagement.SM_URI + semanticModelId;
        try (RDFConnectionFuseki conn = (RDFConnectionFuseki) builder.build()) {
            UpdateBuilder builder = new UpdateBuilder()
                    .addDelete("?s", "?p", "?o")
                    .addWhere("?s", "?p", "?o")
                    .addFilter("STRSTARTS(STR(?s), \"" + prefix + "\")");

            conn.update(builder.build());
        } catch (ParseException e) {
            log.error("Cannot parse semantic model delete query expression", e);
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
