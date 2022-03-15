package de.buw.tmdt.plasma.services.kgs.core;

public class Ontology {

    private String label;
    private String filePath;
    private String prefix;
    private String uri;

    public Ontology(String label, String filePath, String prefix, String uri) {
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
