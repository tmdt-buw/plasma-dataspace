package de.buw.tmdt.plasma.services.dms.shared.api;

public class ModelingInfo {

    private String id;

    private String name;

    private String description;

    private String dataId;

    public ModelingInfo(String id, String name, String description, String dataId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dataId = dataId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDataId() {
        return dataId;
    }
}
