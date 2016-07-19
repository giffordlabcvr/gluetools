package uk.ac.gla.cvr.gluetools.core.jplace;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
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
				Object object = JPlaceResult.jsonValueToObject(value);
				placement.getFieldValues().add(object);
			}
			pQuery.getPlacements().add(placement);
		}
		return pQuery;
	}
	
}
