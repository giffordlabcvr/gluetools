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
package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator.CustomRowCreator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator.CustomTableUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator.PropertyPathInfo;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorContext;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorCustomTableUpdateContext;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorPropertyUpdateContext;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@PluginClass(elemName="genbankXmlPopulator",
	description="Extracts data from Sequences in GenBank XML format, transforms it and uses it to populate fields and relational links")
public class GenbankXmlPopulator extends SequencePopulator<GenbankXmlPopulator> {

	private List<XmlPopulatorRule> rules;
	
	public GenbankXmlPopulator() {
		super();
		registerModulePluginCmdClass(GenbankXmlPopulatorPopulateCommand.class);
		registerModulePluginCmdClass(GenbankXmlPopulatorUpdateCustomTablesCommand.class);
	}

	protected List<XmlPopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		XmlPopulatorRuleFactory populatorRuleFactory = PluginFactory.get(GenbankXmlPopulatorRuleFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		rules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
	}

	private Map<String, PropertyUpdate> populate(CommandContext cmdContext, Sequence sequence, Map<String, PropertyPathInfo> propertyPathToInfo) {
		XmlPopulatorPropertyUpdateContext xmlPopulatorPropertyUpdateContext = new XmlPopulatorPropertyUpdateContext(cmdContext, sequence, propertyPathToInfo);
		runRules(sequence, xmlPopulatorPropertyUpdateContext);
		return xmlPopulatorPropertyUpdateContext.getPropertyUpdates();
	}

	private List<CustomTableUpdate> updateCustomTableRows(CommandContext cmdContext, Sequence sequence) {
		XmlPopulatorCustomTableUpdateContext xmlPopulatorCustomTableUpdateContext = 
				new XmlPopulatorCustomTableUpdateContext(cmdContext, sequence);
		runRules(sequence, xmlPopulatorCustomTableUpdateContext);
		return xmlPopulatorCustomTableUpdateContext.getCustomTableUpdates();
	}

	
	private void runRules(Sequence sequence, XmlPopulatorContext xmlPopulatorContext) {
		String format = sequence.getFormat();
		if(format.equals(SequenceFormat.GENBANK_XML.name())) {
			Document sequenceDataDoc;
			try {
				sequenceDataDoc = GlueXmlUtils.documentFromBytes(sequence.getOriginalData());
			} catch (Exception e) {
				throw new RuntimeException("Bad GENBANK XML format: "+e.getMessage(), e);
			}
			rules.forEach(rule -> {
				rule.execute(xmlPopulatorContext, sequenceDataDoc);
			});
		}
	}

	
	CommandResult updateCustomTables(CommandContext cmdContext, int batchSize, 
			Optional<Expression> whereClause, boolean updateDB, boolean silent) {
		Map<Map<String,String>, List<CustomTableUpdate>> pkMapToUpdates = 
				new LinkedHashMap<Map<String,String>, List<CustomTableUpdate>>();

		processSequences(cmdContext, batchSize, whereClause, updateDB, new SequenceConsumer() {
			@Override
			public void consumeSequence(Sequence sequence) {
				pkMapToUpdates.put(sequence.pkMap(), updateCustomTableRows(cmdContext, sequence));
			}
			@Override
			public void applySequenceUpdates(Sequence seq) {
				List<CustomTableUpdate> updates = pkMapToUpdates.get(seq.pkMap());
				updates.forEach( update -> {
					CustomRowCreator.applyUpdateToDB(cmdContext, update);
				} );
			}
			@Override
			public void batchComplete() {
				if(silent) {
					pkMapToUpdates.clear();
				}
			}
		});
		if(silent) {
			return new OkResult();
		}
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		pkMapToUpdates.forEach((pkMap, updates) -> {
			String sourceName = (String) pkMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) pkMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			updates.forEach(update -> {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				rowData.add(row);
				row.put(Sequence.SOURCE_NAME_PATH, sourceName);
				row.put(Sequence.SEQUENCE_ID_PROPERTY, sequenceID);
				row.put("customTable", update.getTableName());
				row.put("newRowId", update.getNewRowId());
			});
		});
		return new CustomTableResult(rowData);

	}
	
	CommandResult populate(CommandContext cmdContext, int batchSize, 
			Optional<Expression> whereClause, boolean updateDB, boolean silent, List<String> updatableProperties) {
		if(updatableProperties == null) {
			updatableProperties = allUpdatablePropertyPaths();
		}
		Map<String, PropertyPathInfo> propertyPathToInfo = 
				getPropertyPathToInfoMap(cmdContext, updatableProperties);

		Map<Map<String,String>, Map<String, PropertyUpdate>> pkMapToUpdates = 
				new LinkedHashMap<Map<String,String>, Map<String, PropertyUpdate>>();

		processSequences(cmdContext, batchSize, whereClause, updateDB, new SequenceConsumer() {
			@Override
			public void consumeSequence(Sequence sequence) {
				pkMapToUpdates.put(sequence.pkMap(), populate(cmdContext, sequence, propertyPathToInfo));
			}
			@Override
			public void applySequenceUpdates(Sequence seq) {
				Map<String, PropertyUpdate> updates = pkMapToUpdates.get(seq.pkMap());
				updates.values().forEach( update -> {
					PropertyPopulator.applyUpdateToDB(cmdContext, seq, update);
				} );
			}
			@Override
			public void batchComplete() {
				if(silent) {
					pkMapToUpdates.clear();
				}
			}
		});
		if(silent) {
			return new OkResult();
		}
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		pkMapToUpdates.forEach((pkMap, updates) -> {
			String sourceName = (String) pkMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) pkMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			updates.forEach((property, update) -> {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				rowData.add(row);
				row.put(Sequence.SOURCE_NAME_PATH, sourceName);
				row.put(Sequence.SEQUENCE_ID_PROPERTY, sequenceID);
				row.put("property", property);
				row.put("value", update.getValue());
			});
		});
		return new PopulateResult(rowData);
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		for(XmlPopulatorRule rule: rules) {
			rule.validate(cmdContext);
		}
	}


	@Override
	public List<String> allUpdatablePropertyPaths() {
		List<String> paths = new ArrayList<String>();
		for(XmlPopulatorRule rule: rules) {
			paths.addAll(rule.updatablePropertyPaths());
		}
		return paths;
	}


	private static class PopulateResult extends TableResult {

		public PopulateResult(List<Map<String, Object>> rowData) {
			super("gbXmlPopulatorResult", 
					Arrays.asList(Sequence.SOURCE_NAME_PATH, Sequence.SEQUENCE_ID_PROPERTY, "property", "value"), rowData);
		}
		
	}

	private static class CustomTableResult extends TableResult {

		public CustomTableResult(List<Map<String, Object>> rowData) {
			super("gbXmlCustomTableRowsResult", 
					Arrays.asList(Sequence.SOURCE_NAME_PATH, Sequence.SEQUENCE_ID_PROPERTY, "customTable", "newRowId"), rowData);
		}
		
	}
	
	private interface SequenceConsumer {
		public void consumeSequence(Sequence sequence);
		public void applySequenceUpdates(Sequence sequence);
		public void batchComplete();
	}
	
	private void processSequences(CommandContext cmdContext, int batchSize, 
			Optional<Expression> whereClause, boolean updateDB, SequenceConsumer sequenceConsumer) {
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		log("Finding sequences to process");
		int numberToProcess = GlueDataObject.count(cmdContext, selectQuery);
		log("Found "+numberToProcess+" sequences to process");
		List<Sequence> currentSequenceBatch;

		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		int offset = 0;
		while(offset < numberToProcess) {
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize, numberToProcess);
			log("Retrieving sequences "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);
			currentSequenceBatch = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			log("Processing sequences "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);
			for(Sequence sequence: currentSequenceBatch) {
				sequenceConsumer.consumeSequence(sequence);
			}
			if(updateDB) {
				/* DB udpate here */
				currentSequenceBatch.forEach(seq -> {
					sequenceConsumer.applySequenceUpdates(seq);
				});
				cmdContext.commit();
			} 
			cmdContext.newObjectContext();
			sequenceConsumer.batchComplete();
			offset = offset+batchSize;
		}
		log("Processed "+numberToProcess+" sequences");
		cmdContext.newObjectContext();

	}
	
	
}
