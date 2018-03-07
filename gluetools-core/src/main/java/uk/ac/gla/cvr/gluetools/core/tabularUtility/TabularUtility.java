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
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.ResultOutputFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="tabularUtility", 
		description="Provides facilities to the scripting layer for loading and saving tabular data")
public class TabularUtility extends ModulePlugin<TabularUtility>{

	public static final String COLUMN_DELIMITER_REGEX = "columnDelimiterRegex";
	public static final String OUTPUT_FORMAT = "outputFormat";

	private Pattern columnDelimiterRegex;
	private ResultOutputFormat outputFormat;
	

	public TabularUtility() {
		super();
		registerModulePluginCmdClass(LoadTabularCommand.class);
		registerModulePluginCmdClass(SaveTabularCommand.class);
		addSimplePropertyName(COLUMN_DELIMITER_REGEX);
		addSimplePropertyName(OUTPUT_FORMAT);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		columnDelimiterRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, COLUMN_DELIMITER_REGEX, false)).orElse(Pattern.compile("\\t"));
		outputFormat = Optional.ofNullable(PluginUtils.configureEnumProperty(ResultOutputFormat.class, configElem, OUTPUT_FORMAT, false))
				.orElse(ResultOutputFormat.TAB);
	}
	
	public static class TabularData {
		private String[] columnNames;
		private List<String[]> rows;
		
		public TabularData(String[] columnNames, List<String[]> rows) {
			super();
			this.columnNames = columnNames;
			this.rows = rows;
		}

		public String[] getColumnNames() {
			return columnNames;
		}

		public List<String[]> getRows() {
			return rows;
		}
	}
	
	public ResultOutputFormat getOutputFormat() {
		return outputFormat;
	}

	public Pattern getColumnDelimiterRegex() {
		return columnDelimiterRegex;
	}

	public static TabularData tabularDataFromBytes(byte[] bytes, Pattern columnDelimiterRegex) {
		String inputString = new String(bytes);
		String[] allLines = inputString.split("\\r\\n|\\r|\\n");
		String headerLine = allLines[0];
		String[] columnNames = headerLine.split(columnDelimiterRegex.pattern());
		List<String[]> rows = new ArrayList<String[]>();
		for(int i = 1; i < allLines.length; i++) {
			String line = allLines[i];
			if(line.replaceAll(columnDelimiterRegex.pattern(), "").trim().isEmpty()) {
				continue; // only whitespace and column delimiters.
			}
			String[] bits = line.split(columnDelimiterRegex.pattern());
			String[] row = new String[columnNames.length];
			for(int j = 0; j < bits.length; j++) {
				if(j < columnNames.length && !bits[j].trim().isEmpty()) {
					row[j] = bits[j];
				}
			}
			rows.add(row);
		}
		return new TabularData(columnNames, rows);
	}
}
