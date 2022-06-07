package de.buw.tmdt.plasma.services.dps.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.buw.tmdt.plasma.utilities.misc.ObjectUtilities;
import io.hotsprings.eskape.libaries.export.datamodel.TreeNode;
import io.hotsprings.eskape.libraries.slt.format.*;
import io.hotsprings.eskape.pms.shared.tasks.processor.annotations.*;
import io.hotsprings.eskape.pms.shared.tasks.processor.data.Data;
import io.hotsprings.eskape.pms.shared.tasks.processor.data.DataEmitter;
import io.hotsprings.eskape.pms.shared.tasks.processor.exception.EskapeTaskException;
import io.hotsprings.eskape.pms.shared.tasks.processor.types.EskapeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.hotsprings.eskape.tasks.utils.general.TaskContentDescriptor.OUTPUT_EXTRACTION_FORMATTED_DATA;
import static io.hotsprings.eskape.tasks.utils.general.TaskContentDescriptor.OUTPUT_PROCESSING_PAYLOAD;

/**
 * Maps data to JSON objects according to a given structure.
 * Packets are stored until Message has {@code TaskAdditionalDataType.EXTRACTION_IS_LAST_PACKET} set to true.
 * In case of last packet, all packets stored until this point are emitted.
 */
@Input(properties = {
        @Property(name = OUTPUT_PROCESSING_PAYLOAD, type = SLTObject.class)
})
@Output({
        @Property(name = OUTPUT_EXTRACTION_FORMATTED_DATA, type = String.class)
})
@Name("SLT JSON Mapper")
@Description("Maps integrated data points to an array of JSON objects.")
@EskapeTask({EskapeTask.Type.PROCESSING, EskapeTask.Type.FORMATTER})
public class MapJSON implements EskapeProcessor {
    private static final long serialVersionUID = 7241358429325261272L;
    private static final Logger log = LoggerFactory.getLogger(MapJSON.class);
    private static final String JSON_EMPTY = null;

    private static final String JSON_LINE_DELIMITER = "\n";

    private ObjectMapper objectMapper = new ObjectMapper();

    private ObjectMapper mapper = new ObjectMapper();
    private transient TreeNode structure = null;
    private transient DataEmitter dataEmitter;
    private boolean addLineDelimiter = false;

    @UserValue(value = "ADD_LINE_DELIMITER", label = "Add Line Delimiter", description = "Append a line delimiter to each json object",
            order = 1, optional = true, hidden = true)
    public void setAddLineDelimiter(String addLineDelimiter) {
        this.addLineDelimiter = Boolean.parseBoolean(addLineDelimiter);
    }

    @UserValue(value = "JSON_STRUCTURE", label = "JSON structure", description = "The structure of the resulting JSON object, including mappings", order = 0, optional = true, hidden = true)
    public void setStructure(String structure) throws EskapeTaskException {
        try {
            this.structure = mapper.readValue(structure, TreeNode.class);
        } catch (IOException e) {
            log.error("Failed to parse entity mapping", e);
            throw new EskapeTaskException("Failed parse entity mapping", e);
        }
    }

    @Override
    public void initialize(DataEmitter dataEmitter) {
        this.dataEmitter = dataEmitter;
    }

    @Override
    public void execute(Data data) throws EskapeTaskException {
        SLTObject payload = (SLTObject) data.getValue(OUTPUT_PROCESSING_PAYLOAD);
        try {
            ObjectNode resultObject = createObject(structure, ObjectUtilities.deepCopy(payload));
            Data emitData = new Data();
            emitData.addField(OUTPUT_EXTRACTION_FORMATTED_DATA, this.addLineDelimiter ? resultObject.toString() + JSON_LINE_DELIMITER
                    : resultObject.toString());
            dataEmitter.emitData(emitData);
        } catch (ClassCastException | NullPointerException e) {
            log.error("Unexpected field or value in the tuple. Abort execution.", e);
            throw new EskapeTaskException("Values of the input Tuple couldn't be casted correctly.", e);
        } catch (IOException | ClassNotFoundException e) {
            log.error("Couldn't create deepCopy of the ingoing value.", e);
            throw new EskapeTaskException("Couldn't create deepCopy of the ingoing value.", e);
        }
    }

    private ObjectNode createObject(TreeNode structureNode, SLTObject payload) throws EskapeTaskException {
        ObjectNode resultObject = objectMapper.createObjectNode();

        // ensure that there are no duplicate keys for the children
        long distinctCount = structureNode.getChildren().stream().map(TreeNode::getLabel).distinct().count();
        if (distinctCount != structureNode.getChildren().size()) {
            throw new EskapeTaskException("Found duplicate key in structure node " + structureNode);
        }

        for (TreeNode child : structureNode.getChildren()) {
            if (child.isLeaf()) {
                String value = createLeaf(child, payload);
                resultObject.put(child.getLabel(), value);
            } else if (child.isArrayRoot()) {
                resultObject.set(child.getLabel(), createArray(child, payload));
            } else {
                resultObject.set(child.getLabel(), createObject(child, payload));
            }
        }
        return resultObject;

    }

    private String createLeaf(TreeNode structureNode, SLTObject payload) throws EskapeTaskException {
        String mappedEntityId = structureNode.getMappedEntityId();
        if (mappedEntityId == null) {
            return null;
        }
        String value = getValueForEntityId(mappedEntityId, payload);
        if (value == null) {
            throw new EskapeTaskException("Could not obtain value for entityId " + mappedEntityId + " + from SLT");
        }
        return value;

    }

    private ArrayNode createArray(TreeNode structureNode, SLTObject payload) throws EskapeTaskException {
        ArrayNode resultArray = objectMapper.createArrayNode();
        if (!structureNode.isArrayRoot()) {
            throw new EskapeTaskException("TreeNode " + structureNode.getUuid() + " is not declared as array");
        }

        List<String> nodeTypes = structureNode.getChildren().stream()
                .map(c -> {
                    if (c.isLeaf()) {
                        return "leaf";
                    }
                    if (c.isArrayRoot()) {
                        return "array";
                    }
                    return "object";
                })
                .distinct()
                .collect(Collectors.toList());

        if (nodeTypes.size() > 1) {
            throw new EskapeTaskException("Cannot map array " + structureNode.getLabel() + " of different types, currently present: " + nodeTypes);
        }

        for (TreeNode child : structureNode.getChildren()) {
            if (child.isLeaf()) {
                String value = createLeaf(child, payload);
                resultArray.add(value);
            } else if (child.isArrayRoot()) {
                resultArray.add(createArray(child, payload));
            } else {
                resultArray.add(createObject(child, payload));
            }
        }
        return resultArray;
    }

    /**
     * Searches the given SLT node for the needed value for the given entity id.
     * Calls itself recursively if nested.
     *
     * @param entityId The entity id to obtain the value for
     * @param node     The object containing the data
     * @return The value for the given leaf, identified by the EntityType id
     */
    private String getValueForEntityId(String entityId, SLTNode node) {

        if (node instanceof SLTValue) {
            SLTValue value = (SLTValue) node;
            if (value.getEntityType().getLabel().equals(entityId)) {
                return value.getAsString();
            }
            return null;
        }

        if (node instanceof SLTObject) {
            SLTObject value = (SLTObject) node;
            for (Map.Entry<SLTKey, SLTObject.Entry> entry : value.entrySet()) {
                String valueForLeafNode = getValueForEntityId(entityId, entry.getValue());
                if (valueForLeafNode != null) {
                    return valueForLeafNode;
                }
            }
        }

        if (node instanceof SLTValueSet) {
            SLTValueSet valueSet = (SLTValueSet) node;
            for (SLTValue entry : valueSet) {
                String valueForLeafNode = getValueForEntityId(entityId, entry);
                if (valueForLeafNode != null) {
                    return valueForLeafNode;
                }
            }
        }

        if (node instanceof SLTObjectSet) {
            SLTObjectSet valueSet = (SLTObjectSet) node;
            for (SLTObject entry : valueSet) {
                String valueForLeafNode = getValueForEntityId(entityId, entry);
                if (valueForLeafNode != null) {
                    return valueForLeafNode;
                }
            }
        }
        log.warn("Unknown type of SLTNode found");
        return null;
    }
}
