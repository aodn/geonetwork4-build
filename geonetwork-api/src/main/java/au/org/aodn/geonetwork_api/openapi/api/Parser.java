package au.org.aodn.geonetwork_api.openapi.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This parser is used to convert configuration in json to XML, those configuration
 * comes from previous aodn geonetwork3 and is stored in json. When the json is read,
 * the previous provision code convert it to XML and call the Geonetwork API. Hence, we
 * do the exact same thing here.
 */
public class Parser {

    public class Parsed {
        private final JSONObject jsonObject;
        private final String xml;

        public Parsed(JSONObject jsonObject, String xml) {
            this.jsonObject = jsonObject;
            this.xml = xml;
        }

        public String getXml() {
            return this.xml;
        }

        public JSONObject getJsonObject() {
            return this.jsonObject;
        }
    }

    protected Logger logger = LogManager.getLogger(Parser.class);

    public Parsed parseLogosConfig(String json) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(json);
        return new Parsed(
                jsonObject,
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + this.convertJsonToXml(jsonObject, "logo")
        );
    }

    public Parsed parseHarvestersConfig(String json) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(json);
        return new Parsed(
                jsonObject,
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + this.convertJsonToXml(jsonObject.getJSONObject("harvester_data").getJSONObject("node"), "node"));
    }

    protected String convertJsonToXml(JSONObject jsonObject , String rootElement) throws JsonProcessingException {
        StringBuilder xmlBuilder = new StringBuilder("<" + rootElement);

        // Process attributes (keys starting with "@")
        for (String key : jsonObject.keySet()) {
            if (key.startsWith("@")) {
                xmlBuilder.append(" ")
                        .append(key.substring(1))
                        .append("=\"")
                        .append(jsonObject.get(key))
                        .append("\"");
            }
        }
        xmlBuilder.append(">");

        // Process child elements
        for (String key : jsonObject.keySet()) {
            if (!key.startsWith("@")) {
                Object value = jsonObject.get(key);

                if (value instanceof JSONObject) {
                    // Recursive call for nested objects
                    xmlBuilder.append(convertJsonToXml((JSONObject) value, key));
                }
                else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    for (int i = 0; i < array.length(); i++) {
                        Object item = array.get(i);
                        if (item instanceof JSONObject) {
                            // Recursive call for each object in the array
                            xmlBuilder.append(convertJsonToXml((JSONObject) item, key));
                        }
                        else {
                            // Handle non-JSON object items in the array
                            xmlBuilder.append("<").append(key).append(">")
                                    .append(item.toString().replace("&", "&amp;"))
                                    .append("</").append(key).append(">");
                        }
                    }
                }
                else {
                    // Normal element
                    xmlBuilder.append("<")
                            .append(key)
                            .append(">")
                            .append(value.toString().replace("&", "&amp;"))
                            .append("</")
                            .append(key)
                            .append(">");
                }
            }
        }

        xmlBuilder.append("</").append(rootElement).append(">");
        return xmlBuilder.toString();
    }
}
