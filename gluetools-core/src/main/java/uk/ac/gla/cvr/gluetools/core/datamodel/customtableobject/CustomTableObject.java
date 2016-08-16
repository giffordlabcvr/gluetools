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
