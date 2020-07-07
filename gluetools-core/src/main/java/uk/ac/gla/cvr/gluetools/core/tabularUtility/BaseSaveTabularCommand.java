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
package uk.ac.gla.cvr.gluetools.core.tabularUtility;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class BaseSaveTabularCommand<R extends CommandResult> extends ModulePluginCommand<R, TabularUtility>{

	private static final String FILE_NAME = "fileName";
	private static final String TABULAR_DATA = "tabularData";

	private String fileName;
	private Element tabularData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.tabularData = PluginUtils.findConfigElement(configElem, TABULAR_DATA, true);
		PluginUtils.setValidConfigRecursive(this.tabularData);
	}

	
	@Override
	protected final R execute(CommandContext cmdContext, TabularUtility tabularUtility) {
		
		List<Element> childElements = GlueXmlUtils.findChildElements(tabularData);
		if(childElements.size() != 1) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Element tabularInput must have exactly one child element");
		}
		Element childElem = childElements.get(0);
		Document document = GlueXmlUtils.newDocument();
		document.appendChild(document.importNode(childElem, true));
		CommandDocument cmdDocument = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(document);
		String rootTableName = cmdDocument.getRootName();
		CommandArray columnArray = cmdDocument.getArray(BaseTableResult.COLUMN);
		if(columnArray == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No column array");
		}
		List<CommandArrayItem> columns = columnArray.getItems();
		CommandArray rowArray = cmdDocument.getArray(BaseTableResult.ROW);
		List<CommandArrayItem> rows;
		if(rowArray == null) {
			rows = new ArrayList<CommandArrayItem>();
		} else {
			rows = rowArray.getItems();
		}
		ElementTableResult elementTableResult = new ElementTableResult(rootTableName, rows, columns);
		return saveData(cmdContext, tabularUtility, fileName, elementTableResult);
	}

	
	protected abstract R saveData(CommandContext cmdContext, TabularUtility tabularUtility, String fileName, ElementTableResult elementTableResult);
	
	protected static class ElementTableResult extends BaseTableResult<CommandArrayItem> {
		public ElementTableResult(String rootObjectName, List<CommandArrayItem> rows, List<CommandArrayItem> columns) {
			super(rootObjectName, rows, tableColumns(columns));
		}

		@SuppressWarnings("unchecked")
		private static TableColumn<CommandArrayItem>[] tableColumns(
				List<CommandArrayItem> columns) {
			TableColumn<CommandArrayItem>[] tableColumns = new TableColumn[columns.size()];
			for(int i = 0; i < columns.size(); i++) {
				CommandArrayItem columnArrayItem = columns.get(i);
				if((!(columnArrayItem instanceof SimpleCommandValue)) || 
						((SimpleCommandValue) columnArrayItem).getGlueType() != GlueType.String) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Column array item not simple String value");
				}
				String columnName = (String) ((SimpleCommandValue) columnArrayItem).getValue();
				tableColumns[i] = new ElementTableColumn(columnName, i);
			}
			return tableColumns;
		}
		
		private static class ElementTableColumn extends TableColumn<CommandArrayItem> {

			public ElementTableColumn(String columnHeader, final int index) {
				super(columnHeader, cmdAryItm -> {
					if(!(cmdAryItm instanceof CommandObject)) {
						throw new CommandException(Code.COMMAND_FAILED_ERROR, "Row array item not object value");
					}
					CommandArray valueArray = ((CommandObject) cmdAryItm).getArray(BaseTableResult.VALUE);
					if(valueArray == null) {
						throw new CommandException(Code.COMMAND_FAILED_ERROR, "Row object has no 'value' array");
					}
					if(index >= valueArray.size()) {
						return null; // allow rows to have fewer cells than the columns specify.
					}
					CommandArrayItem item = valueArray.getItem(index);
					if(!(item instanceof SimpleCommandValue)) {
						throw new CommandException(Code.COMMAND_FAILED_ERROR, "Value array item is not a simple value");
					}
					return ((SimpleCommandValue) item).getValue();
				});
			}
			
		}
	}
	
	
	
}
