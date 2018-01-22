/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
