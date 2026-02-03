import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

public class Main {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Recursively converts entire JSON structure to a single escaped string.
     * - Objects become "{key=value, ...}" strings
     * - Arrays become "[elem, elem, ...]" strings  
     * - Numbers/booleans become their string representation
     * - Everything is escaped so it can be embedded as a JSON string value
     */
    public static String stringify(JsonNode node) {
        String raw;
        
        if (node instanceof TextNode t) {
            raw = t.asText();
        } else if (node instanceof IntNode i) {
            raw = String.valueOf(i.intValue());
        } else if (node instanceof LongNode l) {
            raw = String.valueOf(l.longValue());
        } else if (node instanceof DoubleNode d) {
            raw = String.valueOf(d.doubleValue());
        } else if (node instanceof FloatNode f) {
            raw = String.valueOf(f.floatValue());
        } else if (node instanceof BigIntegerNode b) {
            raw = b.asText();
        } else if (node instanceof DecimalNode d) {
            raw = d.asText();
        } else if (node instanceof BooleanNode b) {
            raw = String.valueOf(b.booleanValue());
        } else if (node instanceof NullNode) {
            raw = "null";
        } else if (node instanceof ArrayNode arr) {
            var sb = new StringBuilder("[");
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(stringify(arr.get(i)));
            }
            sb.append("]");
            raw = sb.toString();
        } else if (node instanceof ObjectNode obj) {
            var sb = new StringBuilder("{");
            var fields = obj.fields();
            boolean first = true;
            while (fields.hasNext()) {
                var entry = fields.next();
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=")
                  .append(stringify(entry.getValue()));
                first = false;
            }
            sb.append("}");
            raw = sb.toString();
        } else {
            raw = node.toString();
        }
        
        return escapeString(raw);
    }
    
    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    // Usage example
    public static void main(String[] args) throws Exception {
        String json = "{" +
            "\"name\": \"test\"," +
            "\"count\": 42," +
            "\"nested\": {" +
            "    \"value\": 123," +
            "    \"active\": true," +
            "    \"label\": \"Hello \\\"World\\\"\"" +
            "}," +
            "\"items\": [1, 2, 3]" +
            "}";
        
        JsonNode root = mapper.readTree(json);
        String result = stringify(root);
        
        System.out.println("Result:");
        System.out.println(result);
        
        // To use it as a JSON value:
        System.out.println("\nAs JSON field:");
        System.out.println("{\"data\": \"" + result + "\"}");
    }
}
