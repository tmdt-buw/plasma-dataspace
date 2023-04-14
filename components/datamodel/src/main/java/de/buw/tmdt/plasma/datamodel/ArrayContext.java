package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.List;

public class ArrayContext {

    private List<String> relations;
    private List<String> nodes;
    private String rootPath;
    private String indexNodeUuid;
    private long depth;

    public ArrayContext() {
        this.relations = new ArrayList<>();
        this.nodes = new ArrayList<>();
    }

    @JsonCreator
    public ArrayContext(List<String> nodes, List<String> relations, String rootPath, String indexNodeUuid, long depth) {
        this.relations = relations;
        this.nodes = nodes;
        this.rootPath = rootPath;
        this.indexNodeUuid = indexNodeUuid;
        this.depth = depth;
    }

    public List<String> getRelations() {
        return relations;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getIndexNodeUuid() {
        return indexNodeUuid;
    }

    public long getDepth() {
        return depth;
    }
}
