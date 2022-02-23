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
            if (node instanceof Instance) {
                Instance i = (Instance) node;
                Class clazz = i.getSemanticClass();
                Resource resource = m.createResource(SM_Prefix + semanticModelId + "-" + convertToInstanceId(i.getLabel())); // will reuse if already existent
                resource.addProperty(RDF.type, m.createResource(m.expandPrefix(clazz.getURI())));
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
                if (i.getXCoordinate() != null) {
                    resource.addProperty(PLCM.xCoordinate, m.createLiteral(String.valueOf(i.getXCoordinate())));
                }
                if (i.getYCoordinate() != null) {
                    resource.addProperty(PLCM.yCoordinate, m.createLiteral(String.valueOf(i.getYCoordinate())));
                }
                resource.addProperty(PLCM.cmUuid, m.createLiteral(i.getUuid()));
                rdfNode = resource;
            } else if (node instanceof Literal) {
                Literal l = (Literal) node;
                rdfNode = m.createLiteral(l.getValue());
            } else if (node instanceof NamedEntity) {
                NamedEntity ne = (NamedEntity) node;
                Resource resource = m.createResource(m.expandPrefix(ne.getURI()));
                resource.addProperty(RDF.type, PLCM.NamedEntity);
                resource.addProperty(PLCM.label, m.createLiteral(ne.getLabel()));
                if (ne.getDescription() != null && !ne.getDescription().isBlank()) {
                    resource.addProperty(PLCM.description, m.createLiteral(ne.getDescription()));
                }
                if (ne.getXCoordinate() != null) {
                    resource.addProperty(PLCM.xCoordinate, m.createLiteral(String.valueOf(ne.getXCoordinate())));
                }
                if (ne.getYCoordinate() != null) {
                    resource.addProperty(PLCM.yCoordinate, m.createLiteral(String.valueOf(ne.getYCoordinate())));
                }
                rdfNode = resource;
            }
            rdfModel.addProperty(PLCM.hasNode, rdfNode);
            uuidtoResource.put(node.getUuid(), rdfNode);

        });
        semanticModel.getEdges().forEach(r -> {
            SemanticModelNode from = semanticModel.getNode(r.getFromId());
            SemanticModelNode to = semanticModel.getNode(r.getToId());
            RDFNode fromNode = uuidtoResource.get(from.getUuid());
            RDFNode toNode = uuidtoResource.get(to.getUuid());
            fromNode.asResource().addProperty(ResourceFactory.createProperty(r.getURI()), toNode);
        });
        return m;
    }


    public static SemanticModel convertToSemanticModel(Model model) {
        String modelId = null;
        List<SemanticModelNode> nodes = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();

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
            Optional<SemanticModelNode> existingSubject = nodes.stream()
                    .filter(node -> Objects.equals(node.getURI(), s.asResource().getURI()))
                    .findAny();
            SemanticModelNode convertedSubject;
            if (existingSubject.isEmpty()) {
                // we have to create a new one
                convertedSubject = createObject(s.asResource());
                nodes.add(convertedSubject);
            } else {
                convertedSubject = existingSubject.get();
            }

            // check if we can handle the object or if we want to drop this line alltogether
            if (Objects.equals(p.getNameSpace(), PLCM.getURI()) // this is a system statement made by PLASMA
                    || Objects.equals(p, RDF.type)) { // this is a RDF type relation
                continue;
            }

            // handle object before predicate
            SemanticModelNode convertedObject;
            if (o.isResource()) {
                // check if this already exist
                Optional<SemanticModelNode> existingObject = nodes.stream()
                        .filter(node -> Objects.equals(node.getURI(), o.asResource().getURI()))
                        .findAny();
                if (existingObject.isEmpty()) {
                    // we have to create a new one
                    convertedObject = createObject(o.asResource());
                    nodes.add(convertedObject);
                } else {
                    convertedObject = existingObject.get();
                }

            } else if (o.isLiteral()) {
                // create a literal
                convertedObject = new Literal(o.asLiteral().getString());
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

        SemanticModel semanticModel = new SemanticModel(modelId, nodes, relations);
        return semanticModel;

    }

    private static SemanticModelNode createObject(Resource res) {
        if (res.hasProperty(RDF.type, PLCM.NamedEntity)) {
            try {
                return createNamedEntity(res);
            } catch (PropertyNotFoundException pnfe) {
                log.error("Error occurred building named entity from {}", res.getURI(), pnfe);
            }
        } else if (res.hasProperty(RDF.type)) {
            try {
                return createInstance(res);
            } catch (PropertyNotFoundException pnfe) {
                log.error("Error occurred building class from {}", res.getURI(), pnfe);
            }
        }
        throw new CombinedModelIntegrityException("Error occurred building object from " + res.getURI());
    }

    private static Instance createInstance(Resource res) {
        String classUri = res.getRequiredProperty(RDF.type).getObject().asResource().getURI();
        // add only a placeholder class here, needs to be extended later on
        Class sClass = new Class(null, classUri, classUri, null);
        String uuid = res.getRequiredProperty(PLCM.cmUuid).getString();
        String uri = res.getURI();
        String label = res.getRequiredProperty(PLCM.label).getString();
        String description = getPropertyOrNull(res, PLCM.description);
        String syntaxNodeId = getPropertyOrNull(res, PLCM.syntaxNodeId);
        String originalLabel = getPropertyOrNull(res, PLCM.originalLabel);
        Double xCoordinate = getPropertyAsDoubleOrNull(res, PLCM.xCoordinate);
        Double yCoordinate = getPropertyAsDoubleOrNull(res, PLCM.yCoordinate);

        Instance i = new Instance(uuid, uri, label, syntaxNodeId, originalLabel, description, sClass, xCoordinate, yCoordinate);
        return i;
    }

    private static NamedEntity createNamedEntity(Resource res) {
        String uri = res.getURI();
        String label = res.getRequiredProperty(PLCM.label).getString();
        String description = getPropertyOrNull(res, PLCM.description);
        Double xCoordinate = getPropertyAsDoubleOrNull(res, PLCM.xCoordinate);
        Double yCoordinate = getPropertyAsDoubleOrNull(res, PLCM.yCoordinate);
        NamedEntity ne = new NamedEntity(uri, label, description, xCoordinate, yCoordinate);
        return ne;
    }

    public static List<URIDefinedElement> convertToURIDefinedElements(List<Resource> resources) {
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
                        } else if (resource.hasProperty(RDF.type, PLCM.NamedEntity)) {
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
                        } else if (resource.hasProperty(RDF.type, PLCM.NamedEntity)) {
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
