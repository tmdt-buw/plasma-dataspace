package de.buw.tmdt.plasma.services.dms.core.model.datasource;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataSourceModel {

	@Id
	private UUID uuid;

	//The value is only set after the schema analysis
	// Map <Version, CombinedModel>
	private Map<Integer, CombinedModel> combinedModelVersions = new HashMap<>();

	private int versionPointer;


	public DataSourceModel() {
		this.versionPointer = 0;
	}

	public DataSourceModel(UUID uuid) {
		this();
		this.uuid = uuid;
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

	@NotNull
	public CombinedModel getCurrentModel() {
		return combinedModelVersions.get(versionPointer);
	}

	public void popCombinedModel() {
		if (versionPointer == 0) {
			return;
		}
		--this.versionPointer;
	}

	@NotNull
	public CombinedModel restoreCombinedModel() {
		if (versionPointer >= this.combinedModelVersions.size() - 1) {
			throw new RuntimeException("Schema cannot be null");
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