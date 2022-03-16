package de.buw.tmdt.plasma.services.kgs.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Ontology {

    public static final String LABEL_PROPERTY = "label";
    public static final String FILEPATH_PROPERTY = "filePath";
    public static final String PREFIX_PROPERTY = "prefix";
    public static final String URI_PROPERTY = "uri";
    public static final String DESCRIPTION_PROPERTY = "description";

    private String label;

    private String filePath;

    private String prefix;

    private String uri;

    @JsonCreator
    public Ontology(
            @JsonProperty(LABEL_PROPERTY) String label,
            @JsonProperty(FILEPATH_PROPERTY) String filePath,
            @JsonProperty(PREFIX_PROPERTY) String prefix,
            @JsonProperty(URI_PROPERTY) String uri) {
        this.label = label;
        this.filePath = filePath;
        this.prefix = prefix;
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
