package de.buw.tmdt.plasma.services.dss.core.handler;

import de.buw.tmdt.plasma.datamodel.semanticmodel.SemanticModel;
import de.buw.tmdt.plasma.services.dms.shared.feignclient.DataModelingApiClient;
import de.buw.tmdt.plasma.services.dss.core.model.DataSourceModel;
import de.buw.tmdt.plasma.services.dss.core.repository.DataSourceRepository;
import de.buw.tmdt.plasma.services.dss.shared.dto.CreateDataSourceDTO;
import de.buw.tmdt.plasma.services.dss.shared.dto.DataSourceDTO;
import de.buw.tmdt.plasma.services.kgs.shared.feignclient.SemanticModelApiClient;
import de.buw.tmdt.plasma.services.sas.shared.feignclient.SchemaAnalysisApiClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DataSourceHandler {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceHandler.class);

	private final DataSourceRepository dataSourceRepository;
	private final SemanticModelApiClient semanticModelApiClient;
	private final DataModelingApiClient dataModelingAPI;
	private final SchemaAnalysisApiClient analysisApi;

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public DataSourceHandler(
			@NotNull DataSourceRepository dataSourceRepository,
			@NotNull SemanticModelApiClient semanticModelApiClient,
			@NotNull DataModelingApiClient dataModelingAPI,
			@NotNull SchemaAnalysisApiClient analysisApi
	) {
		this.dataSourceRepository = dataSourceRepository;
		this.semanticModelApiClient = semanticModelApiClient;
		this.dataModelingAPI = dataModelingAPI;
		this.analysisApi = analysisApi;
	}

	@NotNull
	public DataSourceDTO createDataSource(@NotNull CreateDataSourceDTO createDataSourceDTO) {
		DataSourceModel dataSourceModel = new DataSourceModel();
		dataSourceModel.setTitle(createDataSourceDTO.getTitle());
		dataSourceModel.setDescription(createDataSourceDTO.getDescription());
		dataSourceModel.setLongDescription(createDataSourceDTO.getLongDescription());
		dataSourceModel.setFileName(createDataSourceDTO.getFilename());

		DataSourceModel newDataSource = dataSourceRepository.save(dataSourceModel);
		return new DataSourceDTO(
				newDataSource.getUuid().toString(),
				newDataSource.getTitle(),
				newDataSource.getDescription(),
				newDataSource.getLongDescription(),
				newDataSource.getFileName()
		);
	}

	@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
	@NotNull
	public DataSourceDTO getDataSource(@NotNull String uuid) {
		Optional<DataSourceModel> newDataSource = dataSourceRepository.findById(UUID.fromString(uuid));
		return newDataSource.map(dataSourceModel -> new DataSourceDTO(
				dataSourceModel.getUuid().toString(),
				dataSourceModel.getTitle(),
				dataSourceModel.getDescription(),
				dataSourceModel.getLongDescription(),
				dataSourceModel.getFileName()
		)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public void deleteDataSource(@NotNull String uuid) {
		//Delete the Semantic Model
		try {
			SemanticModel semanticModel = semanticModelApiClient.getSemanticModel(uuid);
			semanticModelApiClient.deleteSemanticModel(semanticModel.getId());
		} catch (FeignException e) {
			logger.warn("Could not delete semantic model - it might have not exist");
		}

		//Delete the Modeling
		try {
			dataModelingAPI.deleteModel(uuid);
		} catch (FeignException e) {
			logger.warn("Could not delete data schema - it might have not exist");
		}

		//Delete the Schema Analysis
		try {
			analysisApi.delete(uuid);
		} catch (FeignException e) {
			logger.warn("Could not delete schema - it might have not exist");
		}

		//Delete the data source
		dataSourceRepository.deleteById(UUID.fromString(uuid));
	}

	public List<DataSourceDTO> getAllDataSources() {
		return dataSourceRepository.findAll().stream().map(x -> new DataSourceDTO(
				x.getUuid().toString(),
				x.getTitle(),
				x.getDescription(),
				x.getLongDescription(),
				x.getFileName()
		)).collect(Collectors.toList());
	}
}