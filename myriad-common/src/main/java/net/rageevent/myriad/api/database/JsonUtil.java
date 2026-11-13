package net.rageevent.myriad.api.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class JsonUtil {

    public final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ObjectWriter PRETTY = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static String unflatten(String flatJson) throws JsonProcessingException {
        return PRETTY.writeValueAsString(unflatten(OBJECT_MAPPER.readTree(flatJson)));
    }
    public static ObjectNode unflatten(JsonNode flatNode) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();

        for (Map.Entry<String, JsonNode> entry : flatNode.properties()) {
            String key = entry.getKey();
            ObjectNode current = root;
            int start = 0;

            // Walk each dotted segment except the last, creating objects as needed
            for (int dot = key.indexOf('.'); dot != -1; dot = key.indexOf('.', start)) {
                String part = key.substring(start, dot);
                current = current.get(part) instanceof ObjectNode child
                        ? child
                        : current.putObject(part); // creates, or replaces a non-object
                start = dot + 1;
            }

            current.set(key.substring(start), entry.getValue());
        }
        return root;
    }

    public static ObjectNode nest(String dottedPath, JsonNode value) {
        ObjectNode root = JsonUtil.OBJECT_MAPPER.createObjectNode();
        ObjectNode current = root;
        int start = 0;
        for (int dot = dottedPath.indexOf('.'); dot != -1; dot = dottedPath.indexOf('.', start)) {
            current = current.putObject(dottedPath.substring(start, dot));
            start = dot + 1;
        }
        current.set(dottedPath.substring(start), value);
        return root;
    }
    public static JsonNode toMergePatch(String dottedPath, JsonNode node) {
        // Already wrapped? e.g. {"stats":{"kills":5}} for "stats.kills"
        if (node.isObject() && !node.at("/" + dottedPath.replace('.', '/')).isMissingNode()) {
            ((ObjectNode) node).remove("_id");
            return node;
        }
        // Bare value, e.g. 5 or {"kills":5}
        return nest(dottedPath, node);
    }
}
