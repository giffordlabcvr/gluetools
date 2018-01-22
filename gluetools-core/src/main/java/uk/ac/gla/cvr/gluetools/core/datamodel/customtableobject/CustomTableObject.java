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
package uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._CustomTableObject;

/*
 * Had to add the annotation here rather than the generated subclass, as a hack.
 * Here's the stackoverflow question I asked:
 * 
 * http://stackoverflow.com/questions/38899348/annotations-added-to-bcel-generated-java-class-absent-when-class-is-loaded
 */
@GlueDataClass(
		defaultListedProperties = {_CustomTableObject.ID_PROPERTY}, 
		listableBuiltInProperties = {_CustomTableObject.ID_PROPERTY})
public abstract class CustomTableObject extends _CustomTableObject {

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setId(pkMap.get(ID_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getId());
	}

	public static Map<String, String> pkMap(String id) {
		Map<String, String> pkMap = new LinkedHashMap<String,String>();
		pkMap.put(ID_PROPERTY, id);
		return pkMap;
	}
	
}
