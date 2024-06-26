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
package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.

public class ListResult extends TableResult {




	public static final String LIST_RESULT = "listResult";
	public static final String OBJECT_TYPE = "objectType";


	public <D extends GlueDataObject> ListResult(CommandContext cmdContext, Class<D> objectClass, List<D> results) {
		this(cmdContext, objectClass, results, propertyPaths(objectClass));
	}	
	
	public <D extends GlueDataObject> ListResult(CommandContext cmdContext, Class<D> objectClass, List<D> results, List<String> headers) {
		this(cmdContext, objectClass, results, headers, new ListResult.DefaultResolveHeaderFunction<D>(cmdContext));
	}

	
	public <D> ListResult(CommandContext cmdContext, Class<D> objectClass, List<D> results, List<String> headers, 
			BiFunction<D, String, Object> resolveHeaderFunction) {
		super(LIST_RESULT, headers, listOfMapsFromDataObjects(results, headers, resolveHeaderFunction));
		getCommandDocument().set(OBJECT_TYPE, objectClass.getSimpleName());
	}

	
	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		List<String> columnHeaders = super.getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps();
		String objectType = getCommandDocument().getString(OBJECT_TYPE);
		ArrayList<TablePage> tablePages = renderToTablePages(columnHeaders, listOfMaps, renderCtx);
		if(tablePages.size() == 0) {
			renderCtx.output(objectType+"s found: "+listOfMaps.size());
		} else if(tablePages.size() == 1) {
			renderCtx.output(tablePages.get(0).content+objectType+"s found: "+listOfMaps.size());
		} else {
			super.interactiveTableRender(renderCtx, objectType, tablePages);
		}
	}

	
	
	public static <D extends GlueDataObject> List<String> propertyPaths(Class<D> objectClass) {
		return Arrays.asList(Project.getDataClassAnnotation(objectClass).defaultListedProperties());
	}

	public static <D extends GlueDataObject> Object generateResultValue(CommandContext cmdContext, D dataObject, String header) {
		Object nestedPropertyValue = dataObject.readNestedProperty(header);
		if(nestedPropertyValue instanceof GlueDataObject) {
			CommandMode<?> cmdMode = cmdContext.peekCommandMode();
			@SuppressWarnings("unchecked")
			Class<? extends GlueDataObject> theClass = (Class<? extends GlueDataObject>) nestedPropertyValue.getClass();
			if(cmdMode instanceof InsideProjectMode) {
				Project project = ((InsideProjectMode) cmdMode).getProject();
				String tableName = project.getTableNameForDataObjectClass(theClass);
				return project.pkMapToTargetPath(tableName, ((GlueDataObject) nestedPropertyValue).pkMap());
			} else {
				throw new RuntimeException("Can't resolve column value for data object of class "+theClass.getCanonicalName()+" outside of a project mode");
			}
		}
		return nestedPropertyValue;
	}

	
	public static class DefaultResolveHeaderFunction<D extends GlueDataObject> implements BiFunction<D, String, Object> {
		private CommandContext cmdContext;
		
		public DefaultResolveHeaderFunction(CommandContext cmdContext) {
			super();
			this.cmdContext = cmdContext;
		}
	
		@Override
		public Object apply(D dataObject, String header) {
			return generateResultValue(cmdContext, dataObject, header);
		}
	}
	

}
