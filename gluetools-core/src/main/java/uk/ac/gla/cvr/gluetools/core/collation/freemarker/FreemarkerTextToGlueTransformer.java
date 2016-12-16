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

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
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
@PluginClass(elemName="freemarkerTextToGlueTransformer")
public class FreemarkerTextToGlueTransformer extends ModulePlugin<FreemarkerTextToGlueTransformer> {

	public static final String COLUMN_DELIMITER_REGEX = "columnDelimiterRegex";
	public static final String FREEMARKER_TEMPLATE = "freemarkerTemplate";
	
	private Template freemarkerTemplate;
	private Pattern columnDelimiterRegex;
	

	public FreemarkerTextToGlueTransformer() {
		super();
		addModulePluginCmdClass(TransformCommand.class);
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
	
	
	@CommandClass( 
			commandWords={"transform"}, 
			docoptUsages={"<inputFile> (-p | -r [-E] [-C] [-O] | -o <outputFile>)"},
			docoptOptions={
					"-p, --preview                           Preview GLUE commands",
					"-r, --run                               Run GLUE commands",
					"-E, --no-cmd-echo                       Suppress command echo",
					"-C, --no-comment-echo                   Suppress comment echo",
				  	"-O, --no-output                         Suppress result output",
					"-o <outputFile>, --output <outputFile>  Output commands to a file",
			},
			description="Read a CSV file and transform to GLUE using a template", 
			metaTags = { CmdMeta.consoleOnly }
	) 
	public static class TransformCommand extends ModulePluginCommand<CommandResult, FreemarkerTextToGlueTransformer> implements ProvidedProjectModeCommand {

		private String inputFile;
		private String outputFile;
		private boolean preview;
		private boolean run;
		private boolean noCmdEcho;
		private boolean noCommentEcho;
		private boolean noOutput;

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			inputFile = PluginUtils.configureStringProperty(configElem, "inputFile", true);
			outputFile = PluginUtils.configureStringProperty(configElem, "output", false);
			preview = PluginUtils.configureBooleanProperty(configElem, "preview", true);
			noCmdEcho = PluginUtils.configureBooleanProperty(configElem, "no-cmd-echo", true);
			noCommentEcho = PluginUtils.configureBooleanProperty(configElem, "no-comment-echo", true);
			noOutput = PluginUtils.configureBooleanProperty(configElem, "no-output", true);
			run = PluginUtils.configureBooleanProperty(configElem, "run", true);
			if(!( 
					(preview && !run && outputFile == null) ||
					(!preview && run && outputFile == null) ||
					(!preview && !run && outputFile != null)
					)) {
				usageError1();
			}
			if(!run && (noCmdEcho || noOutput)) {
				usageError2();
			}
		}

		private void usageError1() {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either --preview, --run or --output must be specified");
		}

		private void usageError2() {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "The --no-echo and --no-output options may only be used with --run");
		}
		
		@Override
		protected CommandResult execute(CommandContext cmdContext, FreemarkerTextToGlueTransformer tranformerPlugin) {
			return tranformerPlugin.transform((ConsoleCommandContext) cmdContext, inputFile, preview, run, noCmdEcho, noCommentEcho, noOutput, outputFile);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerPathLookup("inputFile", false);
				registerPathLookup("outputFile", false);
			}
		}

		
	}
	
}
