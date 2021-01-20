package de.buw.tmdt.plasma.services.dms.core.services;

import de.buw.tmdt.plasma.services.dms.core.converter.DataSourceSchemaDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.SchemaAnalysisDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.converter.SyntaxNodeDTOConverter;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceModel;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.DataSourceSchema;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.semanticmodel.*;
import de.buw.tmdt.plasma.services.dms.core.model.datasource.syntaxmodel.Node;
import de.buw.tmdt.plasma.services.dms.core.repository.*;
import de.buw.tmdt.plasma.services.dms.shared.dto.DataSourceSchemaDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.EntityTypeDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.RelationConceptDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.RelationDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.semanticmodel.SemanticModelDTO;
import de.buw.tmdt.plasma.services.dms.shared.dto.syntaxmodel.SyntaxModelDTO;
import de.buw.tmdt.plasma.services.sas.shared.dto.syntaxmodel.NodeDTO;
import de.buw.tmdt.plasma.services.sas.shared.feignclient.SchemaAnalysisApiClient;
import de.buw.tmdt.plasma.services.srs.feignclient.SemanticRecommendationApiClient;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataSourceModelInitService {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceModelInitService.class);

	private final EntityTypeRepository entityTypeRepository;
	private final EntityConceptRepository entityConceptRepository;
	private final RelationRepository relationRepository;
	private final RelationConceptRepository relationConceptRepository;
	private final RelationConceptPropertyRepository relationConceptPropertyRepository;
	private final SchemaAnalysisApiClient schemaAnalysisApiClient;
	private final SchemaAnalysisDTOConverter schemaAnalysisDTOConverter;
	private final SemanticRecommendationApiClient semanticRecommendationApiClient;
	private final DataSourceSchemaDTOConverter dataSourceSchemaDTOConverter;
	private final DataSourceRepository dataSourceRepository;

	public DataSourceModelInitService(
			@NotNull EntityTypeRepository entityTypeRepository,
			@NotNull EntityConceptRepository entityConceptRepository,
			@NotNull RelationRepository relationRepository,
			@NotNull RelationConceptRepository relationConceptRepository,
			@NotNull RelationConceptPropertyRepository relationConceptPropertyRepository,
			@NotNull SchemaAnalysisApiClient schemaAnalysisApiClient,
			@NotNull SchemaAnalysisDTOConverter schemaAnalysisDTOConverter,
			SemanticRecommendationApiClient semanticRecommendationApiClient, DataSourceSchemaDTOConverter dataSourceSchemaDTOConverter, DataSourceRepository dataSourceRepository) {
		this.entityTypeRepository = entityTypeRepository;
		this.entityConceptRepository = entityConceptRepository;
		this.relationRepository = relationRepository;
		this.relationConceptRepository = relationConceptRepository;
		this.relationConceptPropertyRepository = relationConceptPropertyRepository;
		this.schemaAnalysisApiClient = schemaAnalysisApiClient;
		this.schemaAnalysisDTOConverter = schemaAnalysisDTOConverter;
		this.semanticRecommendationApiClient = semanticRecommendationApiClient;
		this.dataSourceSchemaDTOConverter = dataSourceSchemaDTOConverter;
		this.dataSourceRepository = dataSourceRepository;
	}

	public DataSourceSchema createInitialSchema(UUID uuid, int exampleLimit, DataSourceModel dataSourceModel) {
		NodeDTO result;
		try {
			result = schemaAnalysisApiClient.getResult(uuid.toString(), exampleLimit);
		} catch (FeignException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not reach SchemaAnalysisService", e);
		}
		SemanticModel semanticModel;
		semanticModel = new SemanticModel(
				uuid.toString(),
				"",
				"",
				new ArrayList<>(),
				new ArrayList<>(),
				null
		);

		DataSourceSchema  schema = new DataSourceSchema(
				dataSourceModel,
				schemaAnalysisDTOConverter.fromNodeDTO(result),
				semanticModel,
				new ArrayList<>(),
				new ArrayList<>()
		);

		dataSourceModel.pushDataSourceSchema(schema);
		dataSourceModel = dataSourceRepository.save(dataSourceModel);

		// request semantic labeling
		DataSourceSchemaDTO dataSourceSchemaDTO1 = dataSourceSchemaDTOConverter.toDTO(dataSourceModel.peekDataSourceSchema());
		dataSourceSchemaDTO1.getSyntaxModelDTO().getNodes().forEach(node -> {
			// no operations when sending to other services
			node.getOperations().clear();
		});
		DataSourceSchemaDTO dataSourceSchemaDTO = semanticRecommendationApiClient.performSemanticLabeling(
				uuid.toString(), dataSourceSchemaDTO1);

		// request semantic modeling
		dataSourceSchemaDTO = semanticRecommendationApiClient.performSemanticModeling(
				uuid.toString(), dataSourceSchemaDTO);

		SemanticModelDTO semanticModelDTO = dataSourceSchemaDTO.getSemanticModelDTO();
		SyntaxModelDTO syntaxModelDTO = dataSourceSchemaDTO.getSyntaxModelDTO();


		Map<Long, Long> entityTypeMapping = new HashMap<>();

		Map<String, EntityConcept> entityConceptMap = new HashMap<>();
		List<EntityType> classEntityTypes = new ArrayList<>();

		List<EntityType> entityTypes = new ArrayList<>();

		for (EntityTypeDTO entityType : semanticModelDTO.getNodes()) {

			if (!entityTypeMapping.containsKey(entityType.getId())) {

				EntityConcept entityConcept;
				if (entityConceptMap.containsKey(entityType.getConcept().getUuid())) {
					entityConcept = entityConceptMap.get(entityType.getConcept().getUuid());
				} else {
					entityConcept = new EntityConcept(
							entityType.getConcept().getUuid(),
							entityType.getConcept().getName(),
							entityType.getConcept().getDescription(),
							entityType.getConcept().getSourceURI(),
							null
					);
					entityConcept = entityConceptRepository.save(entityConcept);
					entityConceptMap.put(entityConcept.getUuid(), entityConcept);
				}

				EntityType newEntityType = new EntityType(
						null,
						entityType.getLabel(),
						entityType.getOriginalLabel(),
						entityType.getDescription(),
						entityConcept,
						null
				);

				newEntityType = entityTypeRepository.save(newEntityType);
				long oldId = entityType.getId();
				long newId = newEntityType.getId();

				entityTypeMapping.put(oldId, newId);
				classEntityTypes.add(newEntityType);
			}
		}

		entityTypes.addAll(classEntityTypes);

		List<Relation> newRelations = new ArrayList<>();
		Map<String, RelationConcept> relationConceptMap = new HashMap<>();

		for (RelationDTO relation : semanticModelDTO.getEdges()) {
			RelationConceptDTO relationConcept = relation.getConcept();

			RelationConcept persistingRelationConcept;

			if (relationConceptMap.containsKey(relation.getConcept().getUuid())) {
				persistingRelationConcept = relationConceptMap.get(relation.getConcept().getUuid());
			} else {
				Set<RelationConcept.Property> properties = new HashSet<>(relationConceptPropertyRepository.findAllByNameIn(relationConcept.getProperties()));
				persistingRelationConcept = new RelationConcept(
						relation.getConcept().getUuid(),
						relation.getConcept().getName(),
						relation.getConcept().getDescription(),
						relation.getConcept().getSourceURI(),
						properties,
						null
				);
				persistingRelationConcept = relationConceptRepository.save(persistingRelationConcept);
				relationConceptMap.put(persistingRelationConcept.getUuid(), persistingRelationConcept);
			}

			long newFromId = entityTypeMapping.get(relation.getFromId());
			long newToId = entityTypeMapping.get(relation.getToId());
			EntityType fromEntity = getEntityTypeById(entityTypes, newFromId);
			EntityType toEntity = getEntityTypeById(entityTypes, newToId);

			Relation newRelation = new Relation(null, fromEntity, toEntity, persistingRelationConcept, relation.getDescription(), null);

			newRelation = relationRepository.save(newRelation);
			newRelations.add(newRelation);
		}

		String uuidS = semanticModelDTO.getUuid() == null ? null : semanticModelDTO.getUuid();

		semanticModel = new SemanticModel(
				uuidS,
				semanticModelDTO.getLabel(),
				semanticModelDTO.getDescription(),
				entityTypes,
				newRelations,
				null
		);

		List<EntityConcept> entityConceptCache = entityTypes.stream().map(EntityType::getEntityConcept).distinct().collect(Collectors.toList());
		List<RelationConcept> relationConceptCache = newRelations.stream().map(Relation::getRelationConcept).distinct().collect(Collectors.toList());

		//Convert the fetched schema to the internal model
		SyntaxNodeDTOConverter converter = new SyntaxNodeDTOConverter(syntaxModelDTO,
				dataSourceSchemaDTO.getPrimitiveEntityTypeEdgeDTOs(), entityTypes, entityTypeMapping);
		Node syntaxModel = converter.convert();

		schema = new DataSourceSchema(
				dataSourceModel,
				syntaxModel,
				semanticModel,
				new ArrayList<>(),
				new ArrayList<>()
		);

		dataSourceModel.pushDataSourceSchema(schema);
		dataSourceModel = dataSourceRepository.save(dataSourceModel);


		return new DataSourceSchema(
				dataSourceModel,
				dataSourceModel.peekDataSourceSchema().getSyntaxModel(),
				dataSourceModel.peekDataSourceSchema().getSemanticModel(),
				entityConceptCache,
				relationConceptCache
		);
	}


	private EntityType getEntityTypeById(List<EntityType> entityTypes, long id) {
		return entityTypes.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
	}
}
