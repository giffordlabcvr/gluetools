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
package uk.ac.gla.cvr.gluetools.core.collation.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;


// module which embeds a Freemarker template.
// This can be applied to a CSV / tab-delimited file to produce GLUE commands.
@PluginClass(elemName="freemarkerTextToGlueTransformer", 
description="Uses FreeMarker to generate GLUE commands from a tabular input file")
public class FreemarkerTextToGlueTransformer extends ModulePlugin<FreemarkerTextToGlueTransformer> {

	public static final String COLUMN_DELIMITER_REGEX = "columnDelimiterRegex";
	public static final String FREEMARKER_TEMPLATE = "freemarkerTemplate";
	
	private Template freemarkerTemplate;
	private Pattern columnDelimiterRegex;
	

	public FreemarkerTextToGlueTransformer() {
		super();
		registerModulePluginCmdClass(FreemarkerTextToGlueTransformerTransformCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		freemarkerTemplate = 
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, FREEMARKER_TEMPLATE, true);
		columnDelimiterRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, COLUMN_DELIMITER_REGEX, false)).orElse(Pattern.compile("\\t"));
	}
	

	public CommandResult transform(ConsoleCommandContext cmdContext, String inputFile, boolean preview, boolean run,
			boolean noCmdEcho, boolean noCommentEcho, boolean noOutput, String outputFile) {
		TextFileModel csvFileModel = modelFromInputFileBytes(cmdContext.loadBytes(inputFile));
		StringWriter stringWriter = new StringWriter();
		try {
			freemarkerTemplate.process(csvFileModel, stringWriter);
		} catch (TemplateException e) {
			throw new FreemarkerTextToGlueException(e, FreemarkerTextToGlueException.Code.TEMPLATE_PROCESSING_FAILED, e.getLocalizedMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String result = stringWriter.toString();
		if(preview) {
			return new SimpleConsoleCommandResult(result);
		}
		if(outputFile != null) {
			cmdContext.saveBytes(outputFile, result.getBytes());
			return new OkResult();
		} else { // run
			cmdContext.runBatchCommands("generated_from_"+inputFile, result, noCmdEcho, noCommentEcho, noOutput);
			return new OkResult();
		}
	}

	private TextFileModel modelFromInputFileBytes(byte[] bytes) {
		String inputString = new String(bytes);
		String[] allLines = inputString.split("\\r\\n|\\r|\\n");
		String headerLine = allLines[0];
		String[] columnNames = headerLine.split(columnDelimiterRegex.pattern());
		List<TextRowModel> csvRowModels = new ArrayList<TextRowModel>();
		for(int i = 1; i < allLines.length; i++) {
			Map<String, TemplateModel> rowFieldValues = new LinkedHashMap<String, TemplateModel>();
			String line = allLines[i];
			if(line.replaceAll(columnDelimiterRegex.pattern(), "").trim().isEmpty()) {
				continue; // only whitespace and column delimiters.
			}
			String[] rowValues = line.split(columnDelimiterRegex.pattern());
			for(int j = 0; j < rowValues.length; j++) {
				if(j < columnNames.length) {
					rowFieldValues.put(columnNames[j], new TextStringValueModel(rowValues[j]));
				}
			}
			// fill in missing columns
			for(int j = rowValues.length; j < columnNames.length; j++) {
				rowFieldValues.put(columnNames[j], new TextStringValueModel(""));
			}
			csvRowModels.add(new TextRowModel(rowFieldValues));
		}
		return new TextFileModel(new TextRowsModel(csvRowModels));
	}
	
	private class TextFileModel implements TemplateHashModel {

		private TextRowsModel rowsModel;
		
		public TextFileModel(TextRowsModel rowsModel) {
			super();
			this.rowsModel = rowsModel;
		}

		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			if(key.equals("rows")) {
				return rowsModel;
			}
			return null;
		}

		@Override
		public boolean isEmpty() throws TemplateModelException {
			return false;
		}

	}

	
	private class TextRowsModel implements TemplateSequenceModel {

		private List<TextRowModel> rows;
		
		public TextRowsModel(List<TextRowModel> rows) {
			super();
			this.rows = rows;
		}

		@Override
		public TemplateModel get(int index) throws TemplateModelException {
			return rows.get(index);
		}

		@Override
		public int size() throws TemplateModelException {
			return rows.size();
		}
	}
	
	private class TextRowModel implements TemplateHashModel {

		private Map<String, TemplateModel> rowFieldValues;
		
		public TextRowModel(Map<String, TemplateModel> rowFieldValues) {
			super();
			this.rowFieldValues = rowFieldValues;
		}

		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			return rowFieldValues.get(key);
		}

		@Override
		public boolean isEmpty() throws TemplateModelException {
			return rowFieldValues.isEmpty();
		}
	}
	
	public class TextStringValueModel implements TemplateScalarModel {
		private String stringValue;

		public TextStringValueModel(String stringValue) {
			super();
			this.stringValue = stringValue;
		}

		@Override
		public String getAsString() throws TemplateModelException {
			return stringValue;
		}
	}
	
}
