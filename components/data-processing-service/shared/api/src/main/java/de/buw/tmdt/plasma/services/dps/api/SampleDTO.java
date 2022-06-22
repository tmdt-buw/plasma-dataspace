package de.buw.tmdt.plasma.services.dps.api;

import java.util.List;

public class SampleDTO {

    private final String dataId;

    private final List<String> samples;

    public SampleDTO(String dataId, List<String> samples) {
        this.dataId = dataId;
        this.samples = samples;
    }

    public String getDataId() {
        return dataId;
    }

    public List<String> getSample() {
        return samples;
    }
}
