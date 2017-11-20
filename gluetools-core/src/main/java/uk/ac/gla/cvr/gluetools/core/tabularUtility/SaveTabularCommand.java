package uk.ac.gla.cvr.gluetools.core.tabularUtility;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
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

@CommandClass(
		commandWords={"save-tabular"}, 
		description = "Save tabular data to a file", 
		docoptUsages = { "" },
		docCategory = "Type-specific module commands",
		metaTags = {CmdMeta.consoleOnly, CmdMeta.inputIsComplex}
)
public class SaveTabularCommand extends ModulePluginCommand<OkResult, TabularUtility>{

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
	protected OkResult execute(CommandContext cmdContext, TabularUtility tabularUtility) {
		
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
		if(rowArray == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "No row array");
		}
		List<CommandArrayItem> rows = rowArray.getItems();
		ElementTableResult elementTableResult = new ElementTableResult(rootTableName, rows, columns);
		((ConsoleCommandContext) cmdContext).saveCommandResult(tabularUtility.getOutputFormat(), fileName, elementTableResult);
		return new OkResult();
	}

	
	private static class ElementTableResult extends BaseTableResult<CommandArrayItem> {
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
