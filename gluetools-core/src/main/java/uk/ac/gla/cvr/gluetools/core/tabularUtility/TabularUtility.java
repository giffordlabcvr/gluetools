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
	public static final String NULL_RENDERING_STRING = "nullRenderingString";
	public static final String TRIM_NULL_VALUES = "trimNullValues";

	private Pattern columnDelimiterRegex;
	private ResultOutputFormat outputFormat;
	private String nullRenderingString;
	private Boolean trimNullValues;
	

	public TabularUtility() {
		super();
		registerModulePluginCmdClass(LoadTabularCommand.class);
		registerModulePluginCmdClass(SaveTabularCommand.class);
		registerModulePluginCmdClass(SaveTabularWebCommand.class);
		addSimplePropertyName(COLUMN_DELIMITER_REGEX);
		addSimplePropertyName(OUTPUT_FORMAT);
		addSimplePropertyName(NULL_RENDERING_STRING);
		addSimplePropertyName(TRIM_NULL_VALUES);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		columnDelimiterRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, COLUMN_DELIMITER_REGEX, false)).orElse(Pattern.compile("\\t"));
		outputFormat = Optional.ofNullable(PluginUtils.configureEnumProperty(ResultOutputFormat.class, configElem, OUTPUT_FORMAT, false))
				.orElse(ResultOutputFormat.TAB);
		nullRenderingString = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, NULL_RENDERING_STRING, false)).orElse("-");
		trimNullValues = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, TRIM_NULL_VALUES, false)).orElse(false);
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

	public String getNullRenderingString() {
		return nullRenderingString;
	}

	public Boolean getTrimNullValues() {
		return trimNullValues;
	}

	public static TabularData tabularDataFromBytes(byte[] bytes, Pattern columnDelimiterRegex) {
		return tabularDataFromBytes(bytes, columnDelimiterRegex, false, null);
	}
	
	public static TabularData tabularDataFromBytes(byte[] bytes, Pattern columnDelimiterRegex, boolean explicitColumnNames, List<String> suppliedColumnNames) {
		String inputString = new String(bytes);
		String[] allLines = inputString.split("\\r\\n|\\r|\\n");
		String[] columnNames;
		int startLine;
		if(explicitColumnNames) {
			columnNames = suppliedColumnNames.toArray(new String[] {});
			startLine = 0;
		} else {
			String headerLine = allLines[0];
			columnNames = headerLine.split(columnDelimiterRegex.pattern());
			startLine = 1;
		}
		List<String[]> rows = new ArrayList<String[]>();
		for(int i = startLine; i < allLines.length; i++) {
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
