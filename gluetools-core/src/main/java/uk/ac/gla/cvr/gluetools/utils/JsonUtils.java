package uk.ac.gla.cvr.gluetools.utils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

}
