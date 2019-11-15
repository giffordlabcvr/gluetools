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

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility.TabularData;

@CommandClass(
		commandWords={"load-tabular"}, 
		description = "Load tabular data from a file", 
		docoptUsages = { "<fileName> [-c (<columnName> ...)]" },
		docoptOptions={
				"-c, --explicitColumnNames  Explicit column names",
		},
		metaTags = {CmdMeta.consoleOnly}	
)
public class LoadTabularCommand extends ModulePluginCommand<TabularResult, TabularUtility>{

	private static final String FILE_NAME = "fileName";
	private static final String EXPLICIT_COLUMN_NAMES = "explicitColumnNames";
	private static final String COLUMN_NAME = "columnName";


	private String fileName;
	private Boolean explicitColumnNames;
	private List<String> columnName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.explicitColumnNames = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXPLICIT_COLUMN_NAMES, false)).orElse(false);
		this.columnName = PluginUtils.configureStringsProperty(configElem, COLUMN_NAME);
		if((!explicitColumnNames) && columnName != null && columnName.size() > 0) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "May not provide column names without --explicitColumnNames option");
		}
		if(explicitColumnNames && (columnName == null || columnName.size() == 0)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --explicitColumnNames option is used then at least one column name must be provided");
		}
	}

	
	@Override
	protected TabularResult execute(CommandContext cmdContext, TabularUtility tabularUtility) {
		byte[] bytes = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		TabularData tabularData = TabularUtility.tabularDataFromBytes(bytes, tabularUtility.getColumnDelimiterRegex(), this.explicitColumnNames, this.columnName);
		return new TabularResult(tabularData);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
