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
package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.utils.RenderUtils;

public abstract class GlueDataObject extends CayenneDataObject {

	protected static final int INDENT = 2;

	private static long timeSpentInDbOperations = 0L;
	public static void resetTimeSpentInDbOperations() {
		timeSpentInDbOperations = 0L;
	}
	public static long getTimeSpentInDbOperations() {
		return timeSpentInDbOperations;
	}
	
	private CommandContext cmdContext;
	
	public abstract void setPKValues(Map <String, String> pkMap);
	
	public CommandContext getCmdContext() {
		return cmdContext;
	}

	protected void setCmdContext(CommandContext cmdContext) {
		this.cmdContext = cmdContext;
	}

	public static <C extends GlueDataObject> C lookup(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap) {
		return lookup(cmdContext, objClass, pkMap, false);
	}
	
	public static <C extends GlueDataObject> C lookup(CommandContext cmdContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C uncommittedObj = cmdContext.lookupUncommitted(objClass, pkMap);
		if(uncommittedObj != null) {
			return uncommittedObj;
		}
		Expression qualifier = pkMapToExpression(pkMap);
		return lookupFromDB(cmdContext, objClass, allowNull, qualifier);
	}
	
	@SuppressWarnings("rawtypes")
	public static <C extends GlueDataObject> C lookupFromDB(
			CommandContext cmdContext, Class<C> objClass, boolean allowNull,
			Expression qualifier) {

		SelectQuery query = new SelectQuery(objClass, qualifier);
		long startTime = System.currentTimeMillis();
		List<?> results = cmdContext.getObjectContext().performQuery(query);
		timeSpentInDbOperations += System.currentTimeMillis() - startTime;
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
		object.setCmdContext(cmdContext);
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
				long startTime = System.currentTimeMillis();
				cmdContext.getObjectContext().deleteObject(object);
				timeSpentInDbOperations += System.currentTimeMillis() - startTime;
			} catch(DeleteDenyException dde) {
				String relationship = dde.getRelationship();
				throw new DataModelException(dde, Code.DELETE_DENIED, objClass.getSimpleName(), pkMap, relationship);
			}
			return new DeleteResult(objClass, 1);
		} else {
			return new DeleteResult(objClass, 0);
		}

	}

	
	@SuppressWarnings("rawtypes")
	public static <C extends GlueDataObject> List<C> query(CommandContext cmdContext, Class<C> objClass, SelectQuery query) {
		try {
			long startTime = System.currentTimeMillis();
			List<?> queryResults = cmdContext.getObjectContext().performQuery(query);
			timeSpentInDbOperations += System.currentTimeMillis() - startTime;
			@SuppressWarnings("unused")
			long start2 = System.currentTimeMillis();
			List<C> classMappedResults = queryResults.stream().map(obj -> { 
				C dataObject = objClass.cast(obj);
				dataObject.setCmdContext(cmdContext);
				return dataObject;
			}).collect(Collectors.toList());
			//System.out.println("Time spent casting results to GlueDataObject class: "+(System.currentTimeMillis() - start2));
			return classMappedResults;
		} catch(Exception e) {
			Expression qualifier = query.getQualifier();
			Throwable prevCause = null;
			Throwable cause = e;
			if(qualifier != null) {
				while(cause != null && cause != prevCause) {
					String causeMessage = cause.getMessage();
					if(cause instanceof ExpressionException) {
						throw new DataModelException(Code.EXPRESSION_ERROR, qualifier.toString(), causeMessage);
					}
					if(cause instanceof CayenneException) {
						if(causeMessage != null) {
							int detailStart = causeMessage.indexOf("Can't resolve path component:");
							if(detailStart >= 0) {
								int detailEnd = causeMessage.indexOf("].", detailStart);
								if(detailEnd >= 0) {
									throw new DataModelException(Code.QUERY_ERROR, qualifier.toString(), causeMessage.substring(detailStart, detailEnd+1));
								}
							}
						}
					}
					prevCause = cause;
					cause = cause.getCause();
				}
			}
			throw e;
		}
	}

	
	@SuppressWarnings("rawtypes")
	public static int count(CommandContext cmdContext, SelectQuery query) {
		try {
			long startTime = System.currentTimeMillis();
			List<?> queryResults = cmdContext.getObjectContext().performQuery(query);
			timeSpentInDbOperations += System.currentTimeMillis() - startTime;
			@SuppressWarnings("unused")
			long start2 = System.currentTimeMillis();
			//System.out.println("Time spent casting results to GlueDataObject class: "+(System.currentTimeMillis() - start2));
			return queryResults.size();
		} catch(CayenneRuntimeException cre) {
			Throwable cause = cre.getCause();
			Expression qualifier = query.getQualifier();
			if(qualifier != null && cause != null && cause instanceof ExpressionException) {
				throw new DataModelException(Code.EXPRESSION_ERROR, qualifier.toString(), cause.getMessage());
			} else {
				throw cre;
			}
		}
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
		long startTime = System.currentTimeMillis();
		C newObject = cmdContext.getObjectContext().newObject(objClass);
		timeSpentInDbOperations += System.currentTimeMillis() - startTime;

		pkMap.values().forEach(pkVal -> {
			if(pkVal.contains("/")) {
				throw new DataModelException(Code.ILLEGAL_PRIMARY_KEY_VALUE, objClass.getSimpleName(), pkVal);
			}
		});
		
		newObject.setPKValues(pkMap);
		newObject.setCmdContext(cmdContext);
		return newObject;
	}

	public String populateListCell(String propertyName) {
		Object readResult = readNestedProperty(propertyName);
		if(readResult == null) {
			return "-";
		}
		return readResult.toString();
	}

	public abstract Map<String, String> pkMap();
	
	protected StringBuffer indent(StringBuffer buf, int indent) {
		for(int i = 0; i < indent; i++) {
			buf.append(" ");
		}
		return buf;
	}

	public final String generateGlueConfig(GlueConfigContext glueConfigContext) {
		StringBuffer glueConfigBuf = new StringBuffer();
		generateGlueConfig(0, glueConfigBuf, glueConfigContext);
		if(glueConfigContext.getCommitAtEnd()) {
			glueConfigBuf.append("commit").append("\n");
		}
		return glueConfigBuf.toString();
	}

	public String renderNestedProperty(String path) {
		return RenderUtils.render(super.readNestedProperty(path));
	}

	public Object renderProperty(String propertyName) {
		return RenderUtils.render(super.readProperty(propertyName));
	}

	public void generateGlueConfig(int i, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
	}

	public String getRenderedName() {
		String result = null;
		if(this instanceof HasDisplayName) {
			result = Optional.ofNullable(((HasDisplayName) this).getDisplayName()).orElse(null);
		}
		if(result == null && this instanceof HasName) {
			result = ((HasName) this).getName();
		}
		return result;
	}

	// this allows custom fields to be correctly handled with dot-notation in freemarker.
	// see docs for BeanModel.TemplateModel get(String key)
	public final Object get(String key) {
		return readProperty(key);
	}
	@Override
	public Object readNestedProperty(String path) {
		try {
			return super.readNestedProperty(path);
		} catch(CayenneRuntimeException cre) {
			Throwable cause = cre;
			if(cre.getCause() != null && cre.getCause() instanceof IntrospectionException) {
				cause = cre.getCause();
			}
			throw new DataModelException(cause, DataModelException.Code.PROPERTY_ERROR, path, cause.getLocalizedMessage());
		}
	}


}
