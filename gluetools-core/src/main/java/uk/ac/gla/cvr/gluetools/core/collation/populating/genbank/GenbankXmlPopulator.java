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

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorContext;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@PluginClass(elemName="genbankXmlPopulator")
public class GenbankXmlPopulator extends SequencePopulator<GenbankXmlPopulator> {

	private List<XmlPopulatorRule> rules;
	
	public GenbankXmlPopulator() {
		super();
		addModulePluginCmdClass(PopulateCommand.class);
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

	private Map<String, FieldUpdate> populate(Sequence sequence, Map<String, FieldType> fieldTypes) {
		XmlPopulatorContext xmlPopulatorContext = new XmlPopulatorContext(sequence, fieldTypes);
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
		return xmlPopulatorContext.getFieldUpdates();
		
	}
	
	private PopulateResult populate(CommandContext cmdContext, int batchSize, 
			Optional<Expression> whereClause, boolean updateDB, List<String> fieldNames) {
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		
		Map<String, FieldType> fieldTypes = getFieldTypes(cmdContext, fieldNames);
		
		log("Finding sequences to process");
		int numberToProcess = GlueDataObject.count(cmdContext, selectQuery);
		log("Found "+numberToProcess+" sequences to process");
		List<Sequence> currentSequenceBatch;

		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		int offset = 0;
		Map<Map<String,String>, Map<String, FieldUpdate>> pkMapToUpdates = new LinkedHashMap<Map<String,String>, Map<String, FieldUpdate>>();
		while(offset < numberToProcess) {
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize+1, numberToProcess);
			log("Retrieving sequences "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);
			currentSequenceBatch = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			log("Processing sequences "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);
			for(Sequence sequence: currentSequenceBatch) {
				pkMapToUpdates.put(sequence.pkMap(), populate(sequence, fieldTypes));
			}
			if(updateDB) {
				/* DB udpate here */
				currentSequenceBatch.forEach(seq -> {
					Map<String, FieldUpdate> updates = pkMapToUpdates.get(seq.pkMap());
					updates.values().forEach( update -> {
						applyUpdateToDB(cmdContext, fieldTypes, seq, update);
					} );
				});
				cmdContext.commit();
			} 
			cmdContext.newObjectContext();
			offset = offset+batchSize;
		}
		log("Processed "+numberToProcess+" sequences");
		cmdContext.newObjectContext();
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		pkMapToUpdates.forEach((pkMap, updates) -> {
			String sourceName = (String) pkMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) pkMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			updates.forEach((fieldName, fieldUpdate) -> {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				rowData.add(row);
				row.put(Sequence.SOURCE_NAME_PATH, sourceName);
				row.put(Sequence.SEQUENCE_ID_PROPERTY, sequenceID);
				row.put("fieldName", fieldName);
				row.put("value", fieldUpdate.getFieldValue());
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


	private static class PopulateResult extends TableResult {

		public PopulateResult(List<Map<String, Object>> rowData) {
			super("gbXmlPopulatorResult", 
					Arrays.asList(Sequence.SOURCE_NAME_PATH, Sequence.SEQUENCE_ID_PROPERTY, "fieldName", "value"), rowData);
		}
		
	}
	
	@CommandClass( 
			commandWords={"populate"}, 
			docoptUsages={"[-b <batchSize>] [-p] [-w <whereClause>] [<fieldName> ...]"},
			docoptOptions={
					"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences",
					"-b <batchSize>, --batchSize <batchSize>        Commit batch size [default: 250]",
					"-p, --preview                                  Database will not be updated"
			},
			metaTags={CmdMeta.updatesDatabase},
			description="Populate sequence field values based on Genbank XML",
			furtherHelp=
			"The <batchSize> argument allows you to control how often updates are committed to the database "+
					"during the import. The default is every 250 sequences. A larger <batchSize> means fewer database "+
					"accesses, but requires more Java heap memory. "+
					"If <fieldName> arguments are supplied, the populator will not update any field unless it appears in the <fieldName> list. "+
					"If no <fieldName> arguments are supplied, the populator may update any field.") 
	public static class PopulateCommand extends ModulePluginCommand<PopulateResult, GenbankXmlPopulator> implements ProvidedProjectModeCommand {

		public static final String BATCH_SIZE = "batchSize";
		public static final String WHERE_CLAUSE = "whereClause";
		public static final String FIELD_NAME = "fieldName";
		public static final String PREVIEW = "preview";

		private Integer batchSize;
		private Optional<Expression> whereClause;
		private List<String> fieldNames;
		private Boolean preview;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
			whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
			preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
			fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
			if(fieldNames.isEmpty()) {
				fieldNames = null; // default fields
			}
		}

		@Override
		protected PopulateResult execute(CommandContext cmdContext, GenbankXmlPopulator populatorPlugin) {
			return populatorPlugin.populate(cmdContext, batchSize, whereClause, !preview, fieldNames);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerVariableInstantiator("fieldName", new ModifiableFieldNameInstantiator(ConfigurableTable.sequence.name()));
			}
		}
		
	}

	
}
