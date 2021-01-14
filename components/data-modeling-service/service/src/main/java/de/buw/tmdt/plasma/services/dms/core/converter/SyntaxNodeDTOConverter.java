package de.buw.tmdt.plasma.services.dms.core.converter;

import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityConcept;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.EntityType;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.*;
import de.buw.tmdt.plasma.services.dms.shared.dto.PrimitiveEntityTypeEdgeDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.EntityConceptDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.EntityTypeDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.syntaxmodel.*;
import de.buw.tmdt.plasma.services.dms.shared.dto.syntaxmodel.operation.DataType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

public class SyntaxNodeDTOConverter {

    private final SyntaxModelDTO syntaxModelDTO;
    private final List<PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs;
    private final List<EntityType> entityTypes;
    private final Map<Long, Long> entityTypeMapping;

    public SyntaxNodeDTOConverter(SyntaxModelDTO syntaxModelDTO,
                                  List<PrimitiveEntityTypeEdgeDTO> primitiveEntityTypeEdgeDTOs,
                                  List<EntityType> entityTypes, Map<Long, Long> entityTypeMapping) {

        this.syntaxModelDTO = syntaxModelDTO;
        this.primitiveEntityTypeEdgeDTOs = primitiveEntityTypeEdgeDTOs;
        this.entityTypes = entityTypes;
        this.entityTypeMapping = entityTypeMapping;
    }

    public Node convert() {

        SchemaNodeDTO root = syntaxModelDTO.getNodes().stream()
                .filter(schemaNodeDTO -> schemaNodeDTO.getUuid().equals(syntaxModelDTO.getRoot()))
                .findFirst().orElseThrow();

        return fromNodeDTO(root);
    }

    @NotNull
    public Node fromNodeDTO(@NotNull SchemaNodeDTO nodeDTO) {
        if (nodeDTO instanceof PrimitiveDTO) {
            return fromPrimitiveDTO((PrimitiveDTO) nodeDTO);
        } else if (nodeDTO instanceof SetNodeDTO) {
            return fromSetNodeDTO((SetNodeDTO) nodeDTO);
        } else if (nodeDTO instanceof ObjectNodeDTO) {
            return fromObjectNodeDTO((ObjectNodeDTO) nodeDTO);
        } else if (nodeDTO instanceof CollisionSchemaDTO) {
            return fromCollisionNodeDTO((CollisionSchemaDTO) nodeDTO);
        } else if (nodeDTO instanceof CompositeDTO) {
            return fromCompositeNodeDTO((CompositeDTO) nodeDTO);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid NodeType");
    }

    @NotNull
    private PrimitiveNode fromPrimitiveDTO(@NotNull PrimitiveDTO primitiveNodeDTO) {
        return new PrimitiveNode(
                entityTypes.stream()
                        .filter(entityType -> {
                            Long oldEntityTypeId = entityTypeMapping.entrySet().stream()
                                    .filter(entry -> entry.getValue().equals(entityType.getId()))
                                    .map(Map.Entry::getKey).findFirst().orElseThrow();

                            PrimitiveEntityTypeEdgeDTO primitiveEntityTypeEdgeDTO = primitiveEntityTypeEdgeDTOs.stream()
                                    .filter(dto -> dto.getPrimitiveUUID().equals(primitiveNodeDTO.getUuid()))
                                    .findFirst()
                                    .orElse(null);

                            if (primitiveEntityTypeEdgeDTO == null) {
                                return false;
                            }

                            return Objects.equals(oldEntityTypeId, primitiveEntityTypeEdgeDTO.getEntityTypeId());
                        })
                        .findFirst()
                        .orElse(null),
                DataType.fromIdentifier(primitiveNodeDTO.getDataType()),
                primitiveNodeDTO.getCleansingPattern(),
                new ArrayList<>(),
                null,
                primitiveNodeDTO.getExamples(),
                primitiveNodeDTO.getUuid()
        );
    }

    @NotNull
    private SetNode fromSetNodeDTO(@NotNull SetNodeDTO primitiveNodeDTO) {
        return new SetNode(
                getChildrenForNode(primitiveNodeDTO).stream().map(node -> new SetNode.Child(null, node)).collect(Collectors.toList()),
                null,
                primitiveNodeDTO.getUuid()
        );
    }

    @NotNull
    private ObjectNode fromObjectNodeDTO(@NotNull ObjectNodeDTO objectNodeDTO) {
        Map<String, Node> children = new HashMap<>();
        getChildrenForNode(objectNodeDTO).forEach(node -> children.put(node.getUuid().toString(), node));
        return new ObjectNode(
                children, null, objectNodeDTO.getUuid()
        );
    }

    private Set<Node> getChildrenForNode(SchemaNodeDTO schemaNodeDTO) {
        List<SchemaNodeDTO> children = syntaxModelDTO.getEdges().stream().filter(edge -> edge.getFrom().equals(schemaNodeDTO.getUuid())).map(
                edge -> syntaxModelDTO.getNodes().stream().filter(node -> node.getUuid().equals(edge.getTo())).findFirst().orElseThrow()
        ).collect(Collectors.toList());

        return children.stream().map(this::fromNodeDTO).collect(Collectors.toSet());
    }

    @NotNull
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "these objects are allowed to be null")
    private CollisionNode fromCollisionNodeDTO(@NotNull CollisionSchemaDTO collisionNodeDTO) {
        return new CollisionNode(
                null,
                null,
                null,
                null,
                collisionNodeDTO.getUuid()
        );
    }

    @NotNull
    private CompositeNode fromCompositeNodeDTO(@NotNull CompositeDTO compositeNodeDTO) {
        return new CompositeNode(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                compositeNodeDTO.getCleansingPattern(),
                null,
                compositeNodeDTO.getUuid()
        );
    }

    @Nullable
    private EntityType fromEntityTypeDTO(@Nullable EntityTypeDTO entityTypeDTO) {
        if (entityTypeDTO == null) {
            return null;
        }
        return new EntityType(
                entityTypeDTO.getUuid(),
                entityTypeDTO.getLabel(),
                entityTypeDTO.getOriginalLabel(),
                entityTypeDTO.getDescription(),
                fromEntityConceptDTO(entityTypeDTO.getConcept()),
                null,
                entityTypeDTO.getId()
        );
    }

    @NotNull
    private EntityConcept fromEntityConceptDTO(@NotNull EntityConceptDTO entityConceptDTO) {
        return new EntityConcept(
                entityConceptDTO.getUuid(),
                entityConceptDTO.getName(),
                entityConceptDTO.getDescription(),
                entityConceptDTO.getSourceURI(),
                null,
                entityConceptDTO.getId()
        );
    }

}