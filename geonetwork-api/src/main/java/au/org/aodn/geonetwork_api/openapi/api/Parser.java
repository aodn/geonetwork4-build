package au.org.aodn.geonetwork_api.openapi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import com.github.underscore.U;

import java.util.Map;

/**
 * This parser is used to convert configuration in json to XML, those configuration
 * comes from previous aodn geonetwork3 and is stored in json. When the json is read,
 * the previous provision code convert it to XML and call the Geonetwork API. Hence, we
 * do the exact same thing here.
 */
public class Parser {

    public static class Parsed {
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
    public Parsed parseLogosConfig(String json) {
        //"<?xml version=\"1.0\" encoding=\"utf-8\"?>" + this.convertJsonToXml(jsonObject, "logo")
        JSONObject jsonObject = new JSONObject(json);
        return new Parsed(
                jsonObject,
                U.jsonToXml(json, "logo")
        );
    }

    public Parsed parseHarvestersConfig(String json) {
        // "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + this.convertJsonToXml(jsonObject.getJSONObject("harvester_data").getJSONObject("node"), "node")
        JSONObject jsonObject = new JSONObject(json);
        return new Parsed(
                jsonObject,
                U.toXml((Map)U.fromJsonMap(json).get("harvester_data"))
        );
    }
}
