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

@PluginClass(elemName="tabularUtility")
public class TabularUtility extends ModulePlugin<TabularUtility>{

	public static final String COLUMN_DELIMITER_REGEX = "columnDelimiterRegex";
	public static final String OUTPUT_FORMAT = "outputFormat";

	private Pattern columnDelimiterRegex;
	private ResultOutputFormat outputFormat;
	

	public TabularUtility() {
		super();
		addModulePluginCmdClass(LoadTabularCommand.class);
		addModulePluginCmdClass(SaveTabularCommand.class);
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

	public TabularData tabularDataFromBytes(byte[] bytes) {
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
