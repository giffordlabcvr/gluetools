package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;

public abstract class GlueDataObject extends CayenneDataObject {

	public abstract String[] populateListRow();
	
	public abstract void setPKValues(Map <String, String> pkMap);

	public static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		return lookup(objContext, objClass, pkMap, false);
	}
	
	public static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C object  = Cayenne.objectForPK(objContext, objClass, pkMap);
		if(object == null && !allowNull) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, objClass.getSimpleName(), pkMap);
		}
		return object;
	}

	public static <C extends GlueDataObject> void delete(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C object = lookup(objContext, objClass, pkMap, allowNull);
		Optional.ofNullable(object).ifPresent(objContext::deleteObject);
	}

	public static <C extends GlueDataObject> void delete(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		delete(objContext, objClass, pkMap, false);
	}
	
	public static <C extends GlueDataObject> C create(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		if(lookup(objContext, objClass, pkMap, true) != null) {
			throw new DataModelException(Code.OBJECT_ALREADY_EXISTS, objClass.getSimpleName(), pkMap);
		}
		C newObject = objContext.newObject(objClass);
		newObject.setPKValues(pkMap);
		return newObject;
	}
	
}
