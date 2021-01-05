package de.buw.tmdt.plasma.services.dms.core.model.datasource;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Document
public class DataSourceModel {

	@Id
	@Indexed
	private UUID uuid;

	//The value is only set after the schema analysis
	// Map <Version, CombinedModel>
	private Map<Integer, CombinedModel> combinedModelVersions = new HashMap<>();

	private int versionPointer;

	public DataSourceModel() {
		this.versionPointer = 0;
		this.uuid = UUID.randomUUID();
	}

	public DataSourceModel(UUID uuid) {
		this();
		this.uuid = uuid;
	}

	@PersistenceConstructor
	private DataSourceModel(UUID uuid, Map<Integer, CombinedModel> combinedModelVersions, int versionPointer) {
		this.uuid = uuid;
		this.combinedModelVersions = combinedModelVersions;
		this.versionPointer = versionPointer;
	}

	@NotNull
	public List<CombinedModel> getAllCombinedModels() {
		return this.combinedModelVersions.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	public Map<Integer, CombinedModel> getAllCombinedModelVersions() {
		return this.combinedModelVersions == null ? new HashMap<>() : this.combinedModelVersions;
	}

	public void pushCombinedModel(@NotNull CombinedModel model) {
		if (versionPointer >= 1 && versionPointer != combinedModelVersions.size()) {
			if (combinedModelVersions.size() > versionPointer) {
				combinedModelVersions.entrySet().removeIf(versionedModel -> versionedModel.getKey() > versionPointer);
			}
		}
		combinedModelVersions.put(++versionPointer, model);
	}

	@NotNull
	public UUID getUuid() {
		return uuid;
	}

	public CombinedModel getCurrentModel() {
		return combinedModelVersions.get(versionPointer);
	}

	public void popCombinedModel() throws UnsupportedOperationException {
		if (versionPointer == 0) {
			throw new UnsupportedOperationException("No schema available.");
		}
		--this.versionPointer;
	}

	@NotNull
	public CombinedModel restoreCombinedModel() throws UnsupportedOperationException {
		if (versionPointer == this.combinedModelVersions.size()) {
			throw new UnsupportedOperationException("Already on latest schema.");
		}
		++this.versionPointer;
		return getCurrentModel();
	}


	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"DataSourceModel\""
				+ ", \"@super\":" + super.toString()
				+ ", \"currentVersion\":" + versionPointer
				+ ", \"maxVersion\":" + combinedModelVersions.size()
				+ '}';
	}
}