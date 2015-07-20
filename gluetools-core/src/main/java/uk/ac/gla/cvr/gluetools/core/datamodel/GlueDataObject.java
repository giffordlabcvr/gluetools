package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;

public abstract class GlueDataObject extends CayenneDataObject {

	private static final String GLUE_DELETED = "glueDeleted";
	private static final String GLUE_NEW = "glueNew";
	private static final String GLUE_MODIFIED = "glueModified";
	
	private ObjectContext myContext;
	private boolean finalized = false;
	
	private ObjectContext getMyContext() {
		return myContext;
	}

	private void setMyContext(ObjectContext myContext) {
		this.myContext = myContext;
	}
	
	private boolean isFinalized() {
		return finalized;
	}

	private void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}

	public static ObjectContext createObjectContext(ServerRuntime serverRuntime) {
		ObjectContext objContext = serverRuntime.getContext();
		objContext.setUserProperty(GLUE_MODIFIED, new LinkedHashMap<CacheKey, GlueDataObject>());
		objContext.setUserProperty(GLUE_NEW, new LinkedHashMap<CacheKey, GlueDataObject>());
		objContext.setUserProperty(GLUE_DELETED, new LinkedHashMap<CacheKey, GlueDataObject>());
		return objContext;
	}
	
	private static <C extends GlueDataObject> C cacheGet(ObjectContext objContext, String mapName,
			Class<C> objClass, Map<String, String> pkMap) {
		@SuppressWarnings("unchecked")
		Map<CacheKey, GlueDataObject> cache = (Map<CacheKey, GlueDataObject>) objContext.getUserProperty(mapName);
		return objClass.cast(cache.get(new CacheKey(objClass, pkMap)));
	}

	private static <C extends GlueDataObject> void cachePut(ObjectContext objContext, String mapName, 
			Map<String, String> pkMap, C object) {
		@SuppressWarnings("unchecked")
		Map<CacheKey, GlueDataObject> cache = (Map<CacheKey, GlueDataObject>) objContext.getUserProperty(mapName);
		cache.put(new CacheKey(object.getClass(), pkMap), object);
	}

	private static <C extends GlueDataObject> void cacheRemove(ObjectContext objContext, String mapName, 
			Map<String, String> pkMap, C object) {
		@SuppressWarnings("unchecked")
		Map<CacheKey, GlueDataObject> cache = (Map<CacheKey, GlueDataObject>) objContext.getUserProperty(mapName);
		cache.remove(new CacheKey(object.getClass(), pkMap));
	}

	public abstract void setPKValues(Map <String, String> pkMap);

	public static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		return lookup(objContext, objClass, pkMap, false);
	}
	
	public static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		
		C cacheNew = cacheGet(objContext, GLUE_NEW, objClass, pkMap);
		if(cacheNew != null) {
			return cacheNew;
		}
		C cacheModified = cacheGet(objContext, GLUE_MODIFIED, objClass, pkMap);
		if(cacheModified != null) {
			return cacheModified;
		}
		C cacheDeleted = cacheGet(objContext, GLUE_DELETED, objClass, pkMap);
		if(cacheDeleted != null) {
			return null;
		}
		
		Expression qualifier = pkMapToExpression(pkMap);
		return lookupFromDB(objContext, objClass, allowNull, qualifier);
	}

	public static <C extends GlueDataObject> C lookupFromDB(
			ObjectContext objContext, Class<C> objClass, boolean allowNull,
			Expression qualifier) {
		SelectQuery query = new SelectQuery(objClass, qualifier);
		List<?> results = objContext.performQuery(query);
		if(results.isEmpty()) {
			if(allowNull) {
				return null;
			} else {
				throw new DataModelException(Code.OBJECT_NOT_FOUND, objClass.getSimpleName(), qualifier.toString());
			}
		}
		if(results.size() > 1) {
			throw new DataModelException(Code.MULTIPLE_OBJECTS_FOUND, objClass.getSimpleName(), qualifier.toString());
		}
		C object = objClass.cast(results.get(0));
		((GlueDataObject) object).setMyContext(objContext);
		return object;
	}

	public static Expression pkMapToExpression(Map<String, String> pkMap) {
		List<Expression> exps = pkMap.entrySet().stream().map(e -> 
			ExpressionFactory.matchExp(e.getKey(), e.getValue())).collect(Collectors.toList());
		Optional<Expression> exp = exps.stream().reduce(Expression::andExp);
		Expression qualifier = exp.get();
		return qualifier;
	}

	public static <C extends GlueDataObject> DeleteResult delete(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C object = lookup(objContext, objClass, pkMap, allowNull);
		if(object != null) {
			objContext.deleteObject(object);
			cacheRemove(objContext, GLUE_NEW, pkMap, object);
			cacheRemove(objContext, GLUE_MODIFIED, pkMap, object);
			cachePut(objContext, GLUE_DELETED, pkMap, object);
			return new DeleteResult(objClass, 1);
		} else {
			return new DeleteResult(objClass, 0);
		}

	}

	public static <C extends GlueDataObject> DeleteResult delete(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		return delete(objContext, objClass, pkMap, false);
	}

	public static <C extends GlueDataObject> C create(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowExists) {
		C existing = lookup(objContext, objClass, pkMap, true);
		if(existing != null) {
			if(allowExists) {
				return existing;
			} else {
				throw new DataModelException(Code.OBJECT_ALREADY_EXISTS, objClass.getSimpleName(), pkMap);
			}
		}
		C newObject = objContext.newObject(objClass);
		cacheRemove(objContext, GLUE_DELETED, pkMap, newObject);
		cacheRemove(objContext, GLUE_MODIFIED, pkMap, newObject);
		cachePut(objContext, GLUE_NEW, pkMap, newObject);
		((GlueDataObject) newObject).setMyContext(objContext);
		newObject.setPKValues(pkMap);
		((GlueDataObject) newObject).setFinalized(true);
		return newObject;
	}
	
	public static <C extends GlueDataObject> C create(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		return create(objContext, objClass, pkMap, false);
	}
	
	public String populateListCell(String propertyName) {
		Object readResult = readNestedProperty(propertyName);
		if(readResult == null) {
			return "-";
		}
		return readResult.toString();
	}

	private static class CacheKey {
		private Class<? extends GlueDataObject> theClass;
		private Map<String, String> pkMap;
		public CacheKey(Class<? extends GlueDataObject> theClass,
				Map<String, String> pkMap) {
			super();
			this.theClass = theClass;
			this.pkMap = pkMap;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pkMap == null) ? 0 : pkMap.hashCode());
			result = prime * result
					+ ((theClass == null) ? 0 : theClass.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (pkMap == null) {
				if (other.pkMap != null)
					return false;
			} else if (!pkMap.equals(other.pkMap))
				return false;
			if (theClass == null) {
				if (other.theClass != null)
					return false;
			} else if (!theClass.equals(other.theClass))
				return false;
			return true;
		}
		
		
	}

	protected abstract Map<String, String> pkMap();
	
	@Override
	public void writePropertyDirectly(String propName, Object val) {
		super.writePropertyDirectly(propName, val);
		if(isFinalized()) {
			Map<String, String> pkMap = pkMap();
			ObjectContext objContext = getMyContext();
			cacheRemove(objContext, GLUE_NEW, pkMap, this);
			cachePut(objContext, GLUE_MODIFIED, pkMap, this);
		}
	}
}
