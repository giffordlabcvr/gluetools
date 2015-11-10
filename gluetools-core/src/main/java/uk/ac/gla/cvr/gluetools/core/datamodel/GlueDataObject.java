package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;

public abstract class GlueDataObject extends CayenneDataObject {

	public abstract void setPKValues(Map <String, String> pkMap);

	public static <C extends GlueDataObject> C lookup(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap) {
		return lookup(cmdContext, objClass, pkMap, false);
	}
	
	public static <C extends GlueDataObject> C lookup(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		Expression qualifier = pkMapToExpression(pkMap);
		return lookupFromDB(cmdContext, objClass, allowNull, qualifier);
	}

	public static <C extends GlueDataObject> C lookupFromDB(
			CommandContext cmdContext, Class<C> objClass, boolean allowNull,
			Expression qualifier) {
		SelectQuery query = new SelectQuery(objClass, qualifier);
		List<?> results = cmdContext.getObjectContext().performQuery(query);
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
		return object;
	}

	public static Expression pkMapToExpression(Map<String, String> pkMap) {
		List<Expression> exps = pkMap.entrySet().stream().map(e -> 
			ExpressionFactory.matchExp(e.getKey(), e.getValue())).collect(Collectors.toList());
		Optional<Expression> exp = exps.stream().reduce(Expression::andExp);
		Expression qualifier = exp.get();
		return qualifier;
	}

	public static <C extends GlueDataObject> DeleteResult delete(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C object = lookup(cmdContext, objClass, pkMap, allowNull);
		if(object != null) {
			try {
				cmdContext.getObjectContext().deleteObject(object);
			} catch(DeleteDenyException dde) {
				String relationship = dde.getRelationship();
				throw new DataModelException(dde, Code.DELETE_DENIED, objClass.getSimpleName(), pkMap, relationship);
			}
			return new DeleteResult(objClass, 1);
		} else {
			return new DeleteResult(objClass, 0);
		}

	}

	
	public static <C extends GlueDataObject> List<C> query(CommandContext cmdContext, Class<C> objClass, SelectQuery query) {
		List<?> queryResult = null;
		try {
			queryResult = cmdContext.getObjectContext().performQuery(query);
		} catch(CayenneRuntimeException cre) {
			Throwable cause = cre.getCause();
			Expression qualifier = query.getQualifier();
			if(qualifier != null && cause != null && cause instanceof ExpressionException) {
				throw new DataModelException(Code.EXPRESSION_ERROR, qualifier.toString(), cause.getMessage());
			} else {
				throw cre;
			}
			
		}
		return queryResult.stream().map(obj -> { 
			C dataObject = objClass.cast(obj);
			return dataObject;
		}).collect(Collectors.toList());
	}
	
	public static <C extends GlueDataObject> C create(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowExists) {
		C existing = lookup(cmdContext, objClass, pkMap, true);
		if(existing != null) {
			if(allowExists) {
				return existing;
			} else {
				throw new DataModelException(Code.OBJECT_ALREADY_EXISTS, objClass.getSimpleName(), pkMap);
			}
		}
		C newObject = cmdContext.getObjectContext().newObject(objClass);
		newObject.setPKValues(pkMap);
		return newObject;
	}
	
	public String populateListCell(String propertyName) {
		Object readResult = readNestedProperty(propertyName);
		if(readResult == null) {
			return "-";
		}
		return readResult.toString();
	}

	protected abstract Map<String, String> pkMap();
	
}
