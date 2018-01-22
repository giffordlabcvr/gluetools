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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CountResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AbstractCountCTableCommand extends ProjectModeCommand<CountResult> {

	public static final String WHERE_CLAUSE = "whereClause";
	private AbstractCountCTableDelegate countCTableDelegate = new AbstractCountCTableDelegate();
	
	protected AbstractCountCTableCommand() {
		super();
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		countCTableDelegate.configure(pluginConfigContext, configElem);
	}
	
	protected void setTableName(String tableName) {
		countCTableDelegate.tableName = tableName;
	}
	
	@Override
	public CountResult execute(CommandContext cmdContext) {
		@SuppressWarnings("unused")
		long startTime = System.currentTimeMillis();
		CountResult listResult = countCTableDelegate.execute(cmdContext);
		// System.out.println("Time spent in AbstractListCTableCommand: "+(System.currentTimeMillis()-startTime));
		return listResult;
	}
	
	public static class CountCommandCompleter extends AdvancedCmdCompleter {

		public CountCommandCompleter(String tableName) {
			super();
		}
	}
		
	
	public static class AbstractCountCTableDelegate {
		private String tableName;
		
		private Optional<Expression> whereClause;
		
		public Optional<Expression> getWhereClause() {
			return whereClause;
		}

		public void setWhereClause(Optional<Expression> whereClause) {
			this.whereClause = whereClause;
		}

		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		}

		@SuppressWarnings("unchecked")
		public <D extends GlueDataObject> CountResult execute(CommandContext cmdContext) {
			SelectQuery selectQuery;
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			Class<D> dataObjectClass = (Class<D>) project.getDataObjectClass(tableName);
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
			} else {
				selectQuery = new SelectQuery(dataObjectClass);
			}
			int count = GlueDataObject.count(cmdContext, selectQuery);
			@SuppressWarnings("unused")
			long startTime = System.currentTimeMillis();
			//System.out.println("Time spent initializing ListResult: "+(System.currentTimeMillis() - startTime));
			return new CountResult(cmdContext, dataObjectClass, count);
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

	}

}
