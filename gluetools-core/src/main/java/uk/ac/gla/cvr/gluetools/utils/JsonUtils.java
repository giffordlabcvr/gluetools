package uk.ac.gla.cvr.gluetools.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;


public class JsonUtils {

	public static JsonObject newObject() {
		return jsonObjectBuilder().build();
	}

	public static JsonObjectBuilder jsonObjectBuilder() {
		return Json.createObjectBuilder();
	}

	public static JsonArrayBuilder jsonArrayBuilder() {
		return Json.createArrayBuilder();
	}

	public static String prettyPrint(JsonObject jsonObject) {
		return print(jsonObject, true);
	}

	public static String print(JsonObject jsonObject, boolean pretty) {
		Map<String, Boolean> config = new LinkedHashMap<String, Boolean>();
		if(pretty) {
			config.put(JsonGenerator.PRETTY_PRINTING, pretty);
		}
        JsonWriterFactory jwf = Json.createWriterFactory(config);
        StringWriter sw = new StringWriter();
        try (JsonWriter jsonWriter = jwf.createWriter(sw)) {
            jsonWriter.writeObject(jsonObject);
        }
        return sw.toString();
   }

	
	public static JsonObject stringToJsonObject(String string) {
		return Json.createReader(new StringReader(string)).readObject();
	}

	public static JsonGenerator jsonGenerator(Writer writer) {
		return Json.createGenerator(writer);
	}
	
}
