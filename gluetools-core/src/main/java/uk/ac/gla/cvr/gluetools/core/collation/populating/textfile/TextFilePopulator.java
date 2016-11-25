package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="textFilePopulator")
public class TextFilePopulator extends SequencePopulator<TextFilePopulator> {

	private static final String SKIP_MISSING = "skipMissing";
	private static final String UPDATE_MULTIPLE = "updateMultiple";
	private static final String COLUMN_DELIMITER_REGEX = "columnDelimiterRegex";
	
	private List<BaseTextFilePopulatorColumn> identifierColumns;
	private List<BaseTextFilePopulatorColumn> headerColumns;
	private List<BaseTextFilePopulatorColumn> numberColumns;
	private boolean skipMissing;
	private boolean updateMultiple;
	private Pattern columnDelimiterRegex;
	
	public TextFilePopulator() {
		super();
		addModulePluginCmdClass(PopulateCommand.class);
		addSimplePropertyName(SKIP_MISSING);
		addSimplePropertyName(UPDATE_MULTIPLE);
		addSimplePropertyName(COLUMN_DELIMITER_REGEX);
		
	}


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		skipMissing = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SKIP_MISSING, false)).orElse(false);
		updateMultiple = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, UPDATE_MULTIPLE, false)).orElse(false);
		columnDelimiterRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, COLUMN_DELIMITER_REGEX, false)).orElse(Pattern.compile("\\t"));
		
		
		TextFilePopulatorColumnFactory populatorColumnFactory = PluginFactory.get(TextFilePopulatorColumnFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(populatorColumnFactory.getElementNames());
		List<Element> columnElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		List<BaseTextFilePopulatorColumn> populatorColumns = populatorColumnFactory.createFromElements(pluginConfigContext, columnElems);

		identifierColumns = populatorColumns.stream().filter(c -> c.getIdentifier().orElse(false)).collect(Collectors.toList());
		if(identifierColumns.isEmpty()) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "At least one column must be an identifier");
		}
		headerColumns = populatorColumns.stream().filter(c -> c.getHeader().isPresent()).collect(Collectors.toList());
		numberColumns = populatorColumns.stream().filter(c -> !c.getHeader().isPresent()).collect(Collectors.toList());
		if(!numberColumns.isEmpty() && !headerColumns.isEmpty()) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "Either all columns must be numbered or none may be");
		}
	}

	
	private OkResult populate(ConsoleCommandContext cmdContext, String fileName, int batchSize) {
		byte[] fileBytes = cmdContext.loadBytes(fileName);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
		TextFilePopulatorContext populatorContext = new TextFilePopulatorContext();
		populatorContext.cmdContext = cmdContext;
		if(!numberColumns.isEmpty()) {
			populatorContext.positionToColumn = new LinkedHashMap<Integer, List<BaseTextFilePopulatorColumn>>();
			populatorContext.columnToPosition = new LinkedHashMap<BaseTextFilePopulatorColumn, Integer>();
			numberColumns.forEach(c -> {
				Integer j = c.getNumber().get();
				populatorContext.positionToColumn.computeIfAbsent(j, x -> new ArrayList<BaseTextFilePopulatorColumn>()).add(c);
				populatorContext.columnToPosition.put(c, j);
			});
		}
		ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
		Project project = projectMode.getProject();
		List<String> definedFieldNames = project.getListableProperties(ConfigurableTable.sequence.name());
		checkFieldsExist(headerColumns, definedFieldNames);
		checkFieldsExist(numberColumns, definedFieldNames);
		LinesProcessedHolder holder = new LinesProcessedHolder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(bais, Charset.forName("UTF-8")));
		reader.lines().forEach(line -> {
			processLine(populatorContext, line);
			holder.linesProcessed++;
			if(holder.linesProcessed % batchSize == 0) {
				log("Processed "+holder.linesProcessed+" lines");
				cmdContext.commit();
				cmdContext.newObjectContext();
			}
		});
		log("Processed "+holder.linesProcessed+" lines");
		cmdContext.commit();
		return CommandResult.OK;
	}

	private class LinesProcessedHolder {
		int linesProcessed = 0;
	}

	private void checkFieldsExist(List<BaseTextFilePopulatorColumn> columns, List<String> definedFieldNames) {
		columns.forEach(col -> {
			String fieldName = col.getFieldName();
			if(!definedFieldNames.contains(fieldName)) {
				throw new TextFilePopulatorException(TextFilePopulatorException.Code.NO_SUCH_FIELD, fieldName, definedFieldNames.toString());
			}
		});
	}
	

	private void processLine(TextFilePopulatorContext populatorContext, String line) {
		if(line.trim().isEmpty()) {
			return;
		}
		String[] cellValues = line.split(columnDelimiterRegex.pattern());
		if(!headerColumns.isEmpty()) {
			if(populatorContext.positionToColumn == null) {
				consumeHeaderLine(populatorContext, cellValues);
				return;
			}
		}
		ConsoleCommandContext cmdContext = populatorContext.cmdContext;
		List<Expression> idExpressions = 
			identifierColumns.stream().map(col -> {
				int j = populatorContext.columnToPosition.get(col);
				String processedCellValue = 
						SequencePopulator.runFieldPopulator(col, cellValues[j]);
				if(processedCellValue == null) {
					throw new TextFilePopulatorException(TextFilePopulatorException.Code.NULL_IDENTIFIER, col.getFieldName());
				}
				return ExpressionFactory.matchExp(col.getFieldName(), processedCellValue);
			}).collect(Collectors.toList());
		Expression identifyingExp = idExpressions.subList(1, idExpressions.size()).
				stream().reduce(idExpressions.get(0), Expression::andExp);
		
		List<Map<String, Object>> sequenceMaps = identifySequences(identifyingExp, cmdContext);
		for(Map<String, Object> seqMap: sequenceMaps) {
			String sourceName = (String) seqMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) seqMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			try (ModeCloser seqMode = cmdContext.pushCommandMode("sequence", sourceName, sequenceID)) {
				for(int i = 0; i < cellValues.length; i++) {
					String cellText = cellValues[i];
					List<BaseTextFilePopulatorColumn> columns = populatorContext.positionToColumn.get(i);
					if(columns != null) {
						for(BaseTextFilePopulatorColumn populatorColumn : columns) {
							if(!populatorColumn.getIdentifier().orElse(false)) {
								populatorColumn.processCellText(populatorContext, cellText);
							}
						}
					}
				}
			}
		}

	}


	public void consumeHeaderLine(TextFilePopulatorContext populatorContext,
			String[] cellValues) {
		populatorContext.positionToColumn = new LinkedHashMap<Integer, List<BaseTextFilePopulatorColumn>>();
		populatorContext.columnToPosition = new LinkedHashMap<BaseTextFilePopulatorColumn, Integer>();
		headerColumns.forEach(c -> {
			for(int i = 0; i < cellValues.length; i++) {
				final int j = i;
				if(c.getHeader().get().equals(cellValues[j])) {
					populatorContext.positionToColumn.computeIfAbsent(j, x -> new ArrayList<BaseTextFilePopulatorColumn>()).add(c);
					populatorContext.columnToPosition.put(c, j);
					return;
				}
			}
			throw new TextFilePopulatorException(TextFilePopulatorException.Code.HEADER_NOT_FOUND, c.getHeader().get());
		});
	}


	public List<Map<String,Object>> identifySequences(Expression identifyingExp, ConsoleCommandContext cmdContext) {
		ListResult listResult = cmdContext.cmdBuilder(ListSequenceCommand.class).
			set(ListSequenceCommand.WHERE_CLAUSE, identifyingExp.toString()).
			execute();
		List<Map<String,Object>> sequenceMaps = listResult.asListOfMaps();
		if(sequenceMaps.size() == 0 && !skipMissing) {
			throw new TextFilePopulatorException(TextFilePopulatorException.Code.NO_SEQUENCE_FOUND, identifyingExp.toString());
		}
		if(sequenceMaps.size() > 1 && !updateMultiple) {
			throw new TextFilePopulatorException(TextFilePopulatorException.Code.MULTIPLE_SEQUENCES_FOUND, identifyingExp.toString());
		}
		return sequenceMaps;
	}

	@CommandClass( 
			commandWords={"populate"}, 
			docoptUsages={"[-b <batchSize>] -f <fileName>"},
			docoptOptions={
				"-b <batchSize>, --batchSize <batchSize>  Commit batch size [default: 250]",
				"-f <fileName>, --fileName <fileName>  Text file with field values"
			},
			description="Populate sequence field values based on a text file", 
			metaTags = { CmdMeta.consoleOnly , CmdMeta.updatesDatabase},
			furtherHelp="The file is loaded from a location relative to the current load/save directory."+
			"The <batchSize> argument allows you to control how often updates are committed to the database "+
					"during the import. The default is every 250 text file lines. A larger <batchSize> means fewer database "+
					"accesses, but requires more Java heap memory.") 
	public static class PopulateCommand extends ModulePluginCommand<OkResult, TextFilePopulator> implements ProvidedProjectModeCommand {

		private Integer batchSize;
		private String fileName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		}
		
		@Override
		protected OkResult execute(CommandContext cmdContext, TextFilePopulator populatorPlugin) {
			return populatorPlugin.populate((ConsoleCommandContext) cmdContext, fileName, batchSize);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerPathLookup("fileName", false);
			}
		}

		
	}

}
