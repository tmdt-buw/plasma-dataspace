package de.buw.tmdt.plasma.services.kgs.core;

import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Class;
import de.buw.tmdt.plasma.datamodel.semanticmodel.Literal;
import de.buw.tmdt.plasma.datamodel.semanticmodel.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static de.buw.tmdt.plasma.services.kgs.core.Ontologies.PREFIXES;

/**
 * Converts a {@link SemanticModel} from a {@link de.buw.tmdt.plasma.datamodel.CombinedModel} to
 * it's RDF representation and vice versa.
 * TODO remove usage of BNodes if possible
 */
public class CMToRDFMapper {

    private static final Logger log = LoggerFactory.getLogger(CMToRDFMapper.class);

    public static Model convertToRDFModel(SemanticModel semanticModel) {
        if (semanticModel.getId() == null) {
            throw new IllegalArgumentException("Semantic model id must not be empty or null");
        }
        Map<String, RDFNode> uuidtoResource = new HashMap<>();
        String semanticModelId = semanticModel.getId();
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefixes(PREFIXES);
        String SM_Prefix = PREFIXES.getNsPrefixURI("plsm");
        Resource rdfModel = m.createResource(SM_Prefix + semanticModelId);
        rdfModel.addProperty(RDF.type, PLCM.SemanticModel);
        rdfModel.addProperty(PLCM.cmUuid, semanticModelId);
        semanticModel.getNodes().forEach(node -> {
            RDFNode rdfNode = null;
            if (node instanceof Class) {
                Class clazz = (Class) node;
                Instance i = clazz.getInstance();
                Resource resource;
                if (i != null) {
                    resource =
                            m.createResource(SM_Prefix + semanticModelId + "-" + convertToInstanceId(i.getLabel())); // will reuse if already existent
                    resource.addProperty(PLCM.label, m.createLiteral(i.getLabel()));
                    if (i.getDescription() != null && !i.getDescription().isBlank()) {
                        resource.addProperty(PLCM.description, m.createLiteral(i.getDescription()));
                    }

                    if (i.getSyntaxNodeUuid() != null) {
                        resource.addProperty(PLCM.syntaxNodeId, m.createLiteral(i.getSyntaxNodeUuid()));
                    }
                    if (i.getOriginalLabel() != null && !i.getOriginalLabel().isBlank()) {
                        resource.addProperty(PLCM.originalLabel, m.createLiteral(i.getOriginalLabel()));
                    }
                } else {
                    resource = m.createResource();
                }
                resource.addProperty(RDF.type, m.createResource(m.expandPrefix(clazz.getURI())));

                if (clazz.getXCoordinate() != null) {
                    resource.addProperty(PLCM.xCoordinate, m.createLiteral(String.valueOf(clazz.getXCoordinate())));
                }
                if (clazz.getYCoordinate() != null) {
                    resource.addProperty(PLCM.yCoordinate, m.createLiteral(String.valueOf(clazz.getYCoordinate())));
                }
                resource.addProperty(PLCM.cmUuid, m.createLiteral(clazz.getUuid()));
                rdfNode = resource;
            } else if (node instanceof Literal) {
                Literal l = (Literal) node;
                rdfNode = m.createLiteral(l.getValue());
            } else if (node instanceof NamedEntity) {
                NamedEntity ne = (NamedEntity) node;
                // create a resource which describes the named entity
                // this will later be elevated to the ontology
                Resource neResource = m.createResource(m.expandPrefix(ne.getURI()));
                neResource.addProperty(RDF.type, PLASMA.NamedEntity);
                neResource.addProperty(PLCM.label, m.createLiteral(ne.getLabel()));
                if (ne.getDescription() != null && !ne.getDescription().isBlank()) {
                    neResource.addProperty(PLCM.description, m.createLiteral(ne.getDescription()));
                }

                // now create an anonymous representation of the named entity in this model
                // this stores local information like the CM uuid and model position
                Resource resource = m.createResource();
                resource.addProperty(RDF.type, neResource);
                resource.addProperty(PLCM.cmUuid, ne.getUuid());
                if (ne.getXCoordinate() != null) {
                    resource.addProperty(PLCM.xCoordinate, m.createLiteral(String.valueOf(ne.getXCoordinate())));
                }
                if (ne.getYCoordinate() != null) {
                    resource.addProperty(PLCM.yCoordinate, m.createLiteral(String.valueOf(ne.getYCoordinate())));
                }
                // use the b-node as reference
                rdfNode = resource;
            }
            rdfModel.addProperty(PLCM.hasNode, rdfNode);
            uuidtoResource.put(node.getUuid(), rdfNode);

        });
        semanticModel.getEdges().forEach(r -> {
            SemanticModelNode from = semanticModel.getNode(r.getFrom());
            SemanticModelNode to = semanticModel.getNode(r.getTo());
            RDFNode fromNode = uuidtoResource.get(from.getUuid());
            RDFNode toNode = uuidtoResource.get(to.getUuid());
            fromNode.asResource().addProperty(ResourceFactory.createProperty(m.expandPrefix(r.getURI())), toNode);
        });
        return m;
    }


    public static SemanticModel convertToSemanticModel(Model model, OntologyManagement onto) {
        String modelId = null;
        List<SemanticModelNode> nodes = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();
        Map<Resource, SemanticModelNode> mappedNodes = new HashMap<>();

        StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.nextStatement();
            Resource s = stmt.getSubject();
            Property p = stmt.getPredicate();
            RDFNode o = stmt.getObject();
            if (s.hasProperty(RDF.type, PLCM.SemanticModel)) {
                // get the model id
                modelId = s.getProperty(PLCM.cmUuid).getString();
                continue;
            }

            // handle subject
            SemanticModelNode convertedSubject = mappedNodes.get(s);

            if (convertedSubject == null) {
                // we have to create a new one
                Resource superclass = s.getRequiredProperty(RDF.type).getObject().asResource();
                SemanticModelNode supernode = onto.findElementsByURI(superclass.getURI());
                convertedSubject = createSemanticModelNode(s, supernode);
                nodes.add(convertedSubject);
                mappedNodes.put(s, convertedSubject);
            }

            // check if we can handle the object or if we want to drop this line all together
            if (Objects.equals(p.getNameSpace(), PLCM.getURI()) // this is a system statement made by PLASMA
                    || Objects.equals(p, RDF.type)) { // this is a RDF type relation
                continue;
            }

            // handle object before predicate
            SemanticModelNode convertedObject;
            if (o.isResource()) {
                Resource object = o.asResource();
                // check if this already exist
                convertedObject = mappedNodes.get(object);
                if (convertedObject == null) {
                    // we have to create a new one
                    Resource superclass = object.getRequiredProperty(RDF.type).getObject().asResource();
                    SemanticModelNode supernode = onto.findElementsByURI(superclass.getURI());
                    convertedObject = createSemanticModelNode(object, supernode);
                    nodes.add(convertedObject);
                    mappedNodes.put(object, convertedObject);
                }

            } else if (o.isLiteral()) {
                // create a literal
                convertedObject = createLiteral(o.asLiteral());
                nodes.add(convertedObject);
            } else {
                throw new CombinedModelIntegrityException("Could not determine type for object " + o);
            }

            // handle properties
            if (o.isResource()) {
                // this is an ObjectProperty
                Relation r = new ObjectProperty(convertedSubject.getUuid(), convertedObject.getUuid(), stmt.getPredicate().getURI());
                relations.add(r);
            } else if (o.isLiteral()) {
                // this is a DataProperty
                Relation r = new DataProperty(convertedSubject.getUuid(), convertedObject.getUuid(), stmt.getPredicate().getURI());
                relations.add(r);
            }
        }
        if(modelId == null){
            // there is no model for that id in the model
            return null;
        }
        SemanticModel semanticModel = new SemanticModel(modelId, nodes, relations);
        return semanticModel;

    }

    private static SemanticModelNode createSemanticModelNode(Resource res, SemanticModelNode entityFromOntology) {
        if (entityFromOntology instanceof NamedEntity) {
            try {
                return createNamedEntity(res, ((NamedEntity) entityFromOntology));
            } catch (PropertyNotFoundException pnfe) {
                log.error("Error occurred building named entity from {}", res.getURI(), pnfe);
            }

        } else if (entityFromOntology instanceof Class) {
            try {
                return createClass(res, (Class) entityFromOntology);
            } catch (PropertyNotFoundException pnfe) {
                log.error("Error occurred building class from {}", res.getURI(), pnfe);
            }
        }
        throw new CombinedModelIntegrityException("Error occurred building object from " + res.getURI());
    }

    private static Class createClass(Resource res, Class superclass) {

        String uuid = res.getRequiredProperty(PLCM.cmUuid).getString();
        Double xCoordinate = getPropertyAsDoubleOrNull(res, PLCM.xCoordinate);
        Double yCoordinate = getPropertyAsDoubleOrNull(res, PLCM.yCoordinate);
        Class sClass = new Class(uuid, superclass.getURI(), superclass.getLabel(),
                superclass.getDescription(), xCoordinate, yCoordinate, null);

        if (!res.isAnon()) {
            // this is an actual instance
            String instanceURI = res.getURI();
            String label = res.getRequiredProperty(PLCM.label).getString();
            String description = getPropertyOrNull(res, PLCM.description);
            String syntaxNodeId = getPropertyOrNull(res, PLCM.syntaxNodeId);
            String originalLabel = getPropertyOrNull(res, PLCM.originalLabel);
            Instance i = new Instance(uuid, instanceURI, label, syntaxNodeId, originalLabel, description);
            sClass.setInstance(i);
        }
        return sClass;
    }

    private static NamedEntity createNamedEntity(Resource res, NamedEntity namedEntity) {
        Double xCoordinate = getPropertyAsDoubleOrNull(res, PLCM.xCoordinate);
        Double yCoordinate = getPropertyAsDoubleOrNull(res, PLCM.yCoordinate);
        NamedEntity ne = new NamedEntity(namedEntity.getURI(), namedEntity.getLabel(),
                namedEntity.getDescription(), xCoordinate, yCoordinate);
        return ne;
    }

    private static Literal createLiteral(org.apache.jena.rdf.model.Literal res) {
        // TODO manage literal positions
        //Double xCoordinate = getPropertyAsDoubleOrNull(res, PLCM.xCoordinate);
        //Double yCoordinate = getPropertyAsDoubleOrNull(res, PLCM.yCoordinate);
        Literal l = new Literal(res.getString());
        return l;
    }

    public static List<SemanticModelNode> convertToSemanticModelNodes(List<Resource> resources) {
        return resources.stream()
                .map(resource -> {
                    try {
                        String label = resource.getRequiredProperty(RDFS.label).getString();
                        String description = getPropertyOrNull(resource, RDFS.comment);
                        if (resource.hasProperty(RDF.type, OWL.Class)) {
                            return new Class(
                                    resource.getURI(),
                                    label,
                                    description);
                        } else if (resource.hasProperty(RDF.type, PLASMA.NamedEntity)) {
                            return new NamedEntity(
                                    resource.getURI(),
                                    label,
                                    description);
                        }
                    } catch (PropertyNotFoundException pnfe) {
                        log.error("Error occurred building class from {}", resource.getURI(), pnfe);

                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String getPropertyOrNull(Resource r, Property property) {
        return r.hasProperty(property) ? r.getProperty(property).getString() : null;
    }

    private static Double getPropertyAsDoubleOrNull(Resource r, Property property) {
        return r.hasProperty(property) ? r.getProperty(property).getDouble() : null;
    }

    public static String convertToInstanceId(String label) {
        String formatted = label.trim().toLowerCase().replace(" ", "_");
        return formatted;
    }

    public static List<Relation> convertToRelations(List<Resource> resources) {
        return resources.stream()
                .map(resource -> {
                    try {
                        String label = resource.getRequiredProperty(RDFS.label).getString();
                        String description = getPropertyOrNull(resource, RDFS.comment);
                        if (resource.hasProperty(RDF.type, OWL.DatatypeProperty)) {
                            return new DataProperty(
                                    null,
                                    null,
                                    resource.getURI(),
                                    label,
                                    description);
                        } else if (resource.hasProperty(RDF.type, PLASMA.NamedEntity)) {
                            return new ObjectProperty(
                                    null,
                                    null,
                                    resource.getURI(),
                                    label,
                                    description);
                        }
                    } catch (PropertyNotFoundException pnfe) {
                        log.error("Error occurred building class from {}", resource.getURI(), pnfe);

                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
