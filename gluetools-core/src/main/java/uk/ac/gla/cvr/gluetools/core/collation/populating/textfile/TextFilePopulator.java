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

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator.PropertyPathInfo;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
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

	
	List<Map<String,String>> populate(ConsoleCommandContext cmdContext, String fileName, int batchSize, Optional<Expression> whereClause, Boolean preview, List<String> updatableProperties) {
		byte[] fileBytes = cmdContext.loadBytes(fileName);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
		TextFilePopulatorContext populatorContext = new TextFilePopulatorContext();
		
		if(updatableProperties == null) {
			updatableProperties = allUpdatablePropertyPaths();
		}
		Map<String, PropertyPathInfo> propertyPathToInfoMap = getPropertyPathToInfoMap(cmdContext, updatableProperties);

		populatorContext.cmdContext = cmdContext;
		populatorContext.whereClause = whereClause;
		populatorContext.updateDB = !preview;
		populatorContext.propertyPathToInfoMap = propertyPathToInfoMap;
		if(!numberColumns.isEmpty()) {
			populatorContext.positionToColumn = new LinkedHashMap<Integer, List<BaseTextFilePopulatorColumn>>();
			populatorContext.columnToPosition = new LinkedHashMap<BaseTextFilePopulatorColumn, Integer>();
			numberColumns.forEach(c -> {
				Integer j = c.getNumber().get();
				populatorContext.positionToColumn.computeIfAbsent(j, x -> new ArrayList<BaseTextFilePopulatorColumn>()).add(c);
				populatorContext.columnToPosition.put(c, j);
			});
		}
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
						ValueExtractor.extractValue(col, cellValues[j]);
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
							String property = populatorColumn.getProperty();
							PropertyPathInfo propertyPathInfo = populatorContext.propertyPathToInfoMap.get(property);
							if(propertyPathInfo == null) {
								continue;
							}
							String extractedValue = ValueExtractor.extractValue(populatorColumn, cellText);
							PropertyUpdate update = PropertyPopulator
									.generatePropertyUpdate(propertyPathInfo, sequence, populatorColumn, extractedValue);

							if(update.updated()) {
								Map<String,String> updateMap = new LinkedHashMap<String,String>();
								updateMap.put(TextFilePopulatorResult.SOURCE_NAME, sourceName);
								updateMap.put(TextFilePopulatorResult.SEQUENCE_ID, sequenceID);
								updateMap.put(TextFilePopulatorResult.PROPERTY, update.getPropertyPathInfo().getPropertyPath());
								updateMap.put(TextFilePopulatorResult.VALUE, update.getValue());
								lineResults.add(updateMap);
								if(populatorContext.updateDB) {
									PropertyPopulator.applyUpdateToDB(cmdContext, sequence, update);
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


	@Override
	public List<String> allUpdatablePropertyPaths() {
		List<String> propertyPaths = new ArrayList<String>();
		headerColumns.forEach(col -> {
			if(!col.getIdentifier().orElse(false)) { 
				propertyPaths.add(col.getProperty()); 
			};
			return;
		});
		numberColumns.forEach(col -> {
			if(!col.getIdentifier().orElse(false)) { 
				propertyPaths.add(col.getProperty()); 
			};
			return;
		});
		return propertyPaths;
	}

}
