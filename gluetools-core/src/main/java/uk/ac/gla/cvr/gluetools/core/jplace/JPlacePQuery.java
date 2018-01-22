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
