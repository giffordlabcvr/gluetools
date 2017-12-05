package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="textFilePopulator",
		description="Populates auxiliary fields and links in the Sequence table from tabular data files")
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
		registerModulePluginCmdClass(TextFilePopulatorPopulateCommand.class);
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

	
	List<Map<String,String>> populate(ConsoleCommandContext cmdContext, String fileName, int batchSize, Optional<Expression> whereClause, Boolean preview, List<String> fieldNames) {
		byte[] fileBytes = cmdContext.loadBytes(fileName);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
		TextFilePopulatorContext populatorContext = new TextFilePopulatorContext();
		populatorContext.cmdContext = cmdContext;
		populatorContext.whereClause = whereClause;
		populatorContext.updateDB = !preview;
		if(fieldNames != null) {
			populatorContext.fieldNames = new LinkedHashSet<String>(fieldNames);
		}
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
		List<String> definedProperties = project.getListableProperties(ConfigurableTable.sequence.name());
		checkPropertiesExist(headerColumns, definedProperties);
		checkPropertiesExist(numberColumns, definedProperties);
		LinesProcessedHolder holder = new LinesProcessedHolder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(bais, Charset.forName("UTF-8")));
		reader.lines().forEach(line -> {
			List<Map<String,String>> lineResults = processLine(populatorContext, line);
			populatorContext.results.addAll(lineResults);
			holder.linesProcessed++;
			if(holder.linesProcessed % batchSize == 0) {
				log("Processed "+holder.linesProcessed+" lines");
				if(!preview) {
					cmdContext.commit();
				}
				cmdContext.newObjectContext();
			}
		});
		log("Processed "+holder.linesProcessed+" lines");
		if(!preview) {
			cmdContext.commit();
		}
		return populatorContext.results;
	}

	private class LinesProcessedHolder {
		int linesProcessed = 0;
	}

	private void checkPropertiesExist(List<BaseTextFilePopulatorColumn> columns, List<String> definedProperties) {
		columns.forEach(col -> {
			String property = col.getProperty();
			if(!definedProperties.contains(property)) {
				throw new TextFilePopulatorException(TextFilePopulatorException.Code.NO_SUCH_PROPERTY, property, definedProperties.toString());
			}
		});
	}
	

	private List<Map<String,String>> processLine(TextFilePopulatorContext populatorContext, String line) {
		List<Map<String,String>> lineResults = new ArrayList<Map<String,String>>();
		if(line.trim().isEmpty()) {
			return lineResults;
		}
		String[] cellValues = line.split(columnDelimiterRegex.pattern());
		if(!headerColumns.isEmpty()) {
			if(populatorContext.positionToColumn == null) {
				consumeHeaderLine(populatorContext, cellValues);
				return lineResults;
			}
		}
		ConsoleCommandContext cmdContext = populatorContext.cmdContext;
		List<Expression> idExpressions = 
			identifierColumns.stream().map(col -> {
				int j = populatorContext.columnToPosition.get(col);
				String processedCellValue = 
						SequencePopulator.runPropertyPopulator(col, cellValues[j]);
				if(processedCellValue == null) {
					throw new TextFilePopulatorException(TextFilePopulatorException.Code.NULL_IDENTIFIER, col.getProperty());
				}
				return ExpressionFactory.matchExp(col.getProperty(), processedCellValue);
			}).collect(Collectors.toList());
		Expression identifyingExp = idExpressions.subList(1, idExpressions.size()).
				stream().reduce(idExpressions.get(0), Expression::andExp);
		
		if(populatorContext.whereClause.isPresent()) {
			identifyingExp = identifyingExp.andExp(populatorContext.whereClause.get());
		}
		
		List<Map<String, Object>> sequenceMaps = identifySequences(identifyingExp, cmdContext);
		Map<String, FieldType> fieldTypes = getFieldTypes(cmdContext, null);
		Map<String, String> links = getLinks(cmdContext, null);
		for(Map<String, Object> seqMap: sequenceMaps) {
			String sourceName = (String) seqMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) seqMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
			for(int i = 0; i < cellValues.length; i++) {
				String cellText = cellValues[i];
				List<BaseTextFilePopulatorColumn> columns = populatorContext.positionToColumn.get(i);
				if(columns != null) {
					for(BaseTextFilePopulatorColumn populatorColumn : columns) {
						if(!populatorColumn.getIdentifier().orElse(false)) {
							if(populatorContext.fieldNames != null && !populatorContext.fieldNames.contains(populatorColumn.getProperty())) {
								continue;
							}
							String fieldPopulatorResult = SequencePopulator.runPropertyPopulator(populatorColumn, cellText);
							String property = populatorColumn.getProperty();
							PropertyUpdate update = SequencePopulator
									.generatePropertyUpdate(fieldTypes.get(property), links.get(property), sequence, populatorColumn, fieldPopulatorResult);

							if(update != null && update.updated()) {
								Map<String,String> updateMap = new LinkedHashMap<String,String>();
								updateMap.put(TextFilePopulatorResult.SOURCE_NAME, sourceName);
								updateMap.put(TextFilePopulatorResult.SEQUENCE_ID, sequenceID);
								updateMap.put(TextFilePopulatorResult.PROPERTY, update.getProperty());
								updateMap.put(TextFilePopulatorResult.VALUE, update.getValue());
								lineResults.add(updateMap);
								if(populatorContext.updateDB) {
									super.applyUpdateToDB(cmdContext, fieldTypes, links, sequence, update);
								}
							}
						}
					}
				}
			}
		}
		return lineResults;
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

}
