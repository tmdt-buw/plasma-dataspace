package de.buw.tmdt.plasma.services.dps.api;

import java.util.List;

public class SampleDTO {

    private final String dataId;

    private final List<String> sample;

    public SampleDTO(String dataId, List<String> sample) {
        this.dataId = dataId;
        this.sample = sample;
    }

    public String getDataId() {
        return dataId;
    }

    public List<String> getSample() {
        return sample;
    }
}
