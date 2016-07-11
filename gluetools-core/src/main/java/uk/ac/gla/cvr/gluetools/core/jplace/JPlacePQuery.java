package uk.ac.gla.cvr.gluetools.core.jplace;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

public abstract class JPlacePQuery {

	private List<JPlacePlacement> placements = new ArrayList<JPlacePlacement>();

	public List<JPlacePlacement> getPlacements() {
		return placements;
	}

	public void setPlacements(List<JPlacePlacement> placements) {
		this.placements = placements;
	}

	public static JPlacePQuery parse(JsonObject jsonObject) {
		JPlacePQuery pQuery;
		if(jsonObject.containsKey("nm")) {
			pQuery = new JPlaceNameMultiplicityPQuery();
			JsonArray nmJsonArray = jsonObject.getJsonArray("nm");
			for(JsonValue nmJsonValue : nmJsonArray) {
				JPlaceNameMultiplicity nameMultiplicity = new JPlaceNameMultiplicity();
				nameMultiplicity.setName(((JsonArray) nmJsonValue).getString(0));
				nameMultiplicity.setMultiplicity(((JsonArray) nmJsonValue).getJsonNumber(1).doubleValue());
				((JPlaceNameMultiplicityPQuery) pQuery).getNameMultiplicities().add(nameMultiplicity);
			}
		} else {
			pQuery = new JPlaceNamePQuery();
			JsonArray nJsonArray = jsonObject.getJsonArray("n");
			for(JsonValue nValue : nJsonArray) {
				((JPlaceNamePQuery) pQuery).getNames().add(((JsonString) nValue).getString());
			}
		}
		JsonArray pJsonArray = jsonObject.getJsonArray("p");
		for(JsonValue pJsonValue : pJsonArray) {
			JsonArray valuesArray = ((JsonArray) pJsonValue);
			JPlacePlacement placement = new JPlacePlacement();
			for(JsonValue value: valuesArray) {
				Object object;
				switch(value.getValueType()) {
				case NUMBER:
					JsonNumber number = (JsonNumber) value;
					if(number.isIntegral()) {
						object = number.intValue();
					} else {
						object = number.doubleValue();
					}
					break;
				case STRING:
					object = ((JsonString) value).getString();
					break;
				case TRUE:
					object = new Boolean(true);
					break;
				case FALSE:
					object = new Boolean(false);
					break;
				default:
					throw new RuntimeException("Unexpected JSON type in p array");
				}
				placement.getFieldValues().add(object);
			}
			pQuery.getPlacements().add(placement);
		}
		return pQuery;
	}
	
}
