package de.buw.tmdt.plasma.services.sms.modeling;

import de.buw.tmdt.plasma.datamodel.generators.AdvancedCombinedModelGenerator;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Literal;
import de.buw.tmdt.plasma.datamodel.semanticmodel.DataProperty;
import de.buw.tmdt.plasma.services.sms.kg.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SemanticModelServiceTest {
    @Test void preservesMappedLiteralsAndArrayContextsThroughStorage() {
        var generator = new AdvancedCombinedModelGenerator(); generator.setMapStaffInstance(true);
        var original = generator.getFlightModel().getSemanticModel();
        var ops = mock(SemanticModelManagement.class); var onto = mock(LocalOntologyManagement.class); var ontology = mock(OntologyService.class);
        var stored = new AtomicReference<Model>();
        doAnswer(call -> { stored.set(ModelFactory.createDefaultModel().add((Model) call.getArgument(0))); return null; }).when(ops).persistModel(any());
        when(ops.queryModel(original.getId())).thenAnswer(call -> stored.get());
        when(ontology.getElements(anyList(), isNull(), isNull(), anyString())).thenAnswer(call -> original.getNodes().stream().filter(n -> call.getArgument(3).equals(n.getURI())).toList());
        when(ontology.getRelations(anyList(), isNull(), isNull(), anyString())).thenAnswer(call -> original.getEdges().stream().filter(n -> call.getArgument(3).equals(n.getURI())).toList());
        var service = new SemanticModelService(ops, onto, ontology);
        service.storeSemanticModel(original);
        var restored = service.getSemanticModel(original.getId());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getNodes().size(), restored.getNodes().size());
        assertEquals(original.getEdges().size(), restored.getEdges().size());
        for (var node : original.getNodes()) {
            if (node instanceof Literal literal && literal.isMapped()) {
                var copy = (Literal) restored.getNode(node.getUuid());
                assertEquals(literal.getMappedSyntaxNodeUuid(), copy.getMappedSyntaxNodeUuid());
                assertEquals(literal.getMappedSyntaxNodePath(), copy.getMappedSyntaxNodePath());
                assertEquals(literal.getLabel(), copy.getLabel());
            }
        }
        assertEquals(original.getEdges().stream().filter(e -> e.isArrayContext()).count(), restored.getEdges().stream().filter(e -> e.isArrayContext()).count());
        assertEquals(original.getEdges().stream().filter(e -> e instanceof DataProperty).count(), restored.getEdges().stream().filter(e -> e instanceof DataProperty).count());
        service.deleteSemanticModel(original.getId()); verify(ops).deleteModel(original.getId());
    }
}
