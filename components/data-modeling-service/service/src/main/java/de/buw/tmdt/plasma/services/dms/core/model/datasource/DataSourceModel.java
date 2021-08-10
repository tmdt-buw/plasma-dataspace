package de.buw.tmdt.plasma.services.dms.core.model.datasource;

import de.buw.tmdt.plasma.datamodel.CombinedModel;
import de.buw.tmdt.plasma.services.dms.core.database.CombinedModelPersistenceConverter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.annotations.DynamicUpdate;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@DynamicUpdate
@Entity
@Table(name = "data_sources")
public class DataSourceModel {

    @Id
    @Column(nullable = false, unique = true, updatable = false, length = 16)
    @NotNull
    private UUID uuid;

    //The value is only set after the schema analysis
    // Map <Version, CombinedModel>
    @SuppressWarnings("JpaAttributeTypeInspection") // will be custom serialized
    @ElementCollection
    @Convert(converter = CombinedModelPersistenceConverter.class)
    @Lob
    @NotNull
    private List<@NotNull CombinedModel> combinedModelVersions = new ArrayList<>();

    @Column(nullable = false)
    private int versionPointer;

    public DataSourceModel() {
        this.versionPointer = 0;
        this.uuid = UUID.randomUUID();
    }

    public DataSourceModel(UUID uuid) {
        this();
        this.uuid = uuid;
    }

    @NotNull
    public List<CombinedModel> getAllCombinedModels() {
        return this.combinedModelVersions;
    }

    public void setAllCombinedModels(@NotNull List<CombinedModel> models) {
        combinedModelVersions.clear();
        models.stream().filter(Objects::nonNull).forEach(combinedModelVersions::add);
        versionPointer = combinedModelVersions.size();
    }

    public void pushCombinedModel(@NotNull CombinedModel model) {
        if (versionPointer >= 1 && versionPointer != combinedModelVersions.size()) {
            if (combinedModelVersions.size() > versionPointer - 1) {
                combinedModelVersions.subList(versionPointer, combinedModelVersions.size()).clear();
            }
        }
        combinedModelVersions.add(model);
        versionPointer++;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @Transient
    @NotNull
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION") // all values are not null
    public CombinedModel getCurrentModel() {
        if (versionPointer == 0 || combinedModelVersions.isEmpty()) {
            throw new UnsupportedOperationException("No model available.");
        }
        try {
            return combinedModelVersions.get(versionPointer - 1);
        } catch (IndexOutOfBoundsException e) {
            // somehow the versions got messed up, reset to latest version
            versionPointer = combinedModelVersions.size();
            return combinedModelVersions.get(versionPointer - 1);
        }
    }

    public void popCombinedModel() throws UnsupportedOperationException {
        if (versionPointer == 0) {
            throw new UnsupportedOperationException("No model available.");
        }
        if (versionPointer == 1) {
            throw new UnsupportedOperationException("Already on first version.");
        }
        --this.versionPointer;
    }

    @NotNull
    public CombinedModel restoreCombinedModel() throws UnsupportedOperationException {
        if (versionPointer == this.combinedModelVersions.size()) {
            throw new UnsupportedOperationException("Already on latest model.");
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