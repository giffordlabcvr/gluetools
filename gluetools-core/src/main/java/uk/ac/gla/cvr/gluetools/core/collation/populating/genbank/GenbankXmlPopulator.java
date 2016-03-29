package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorContext;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
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

	private void populate(CommandContext cmdContext, String sourceName, String sequenceID, String format, 
			List<Map<String, Object>> rowData, List<String> fieldNames) {
		XmlPopulatorContext xmlPopulatorContext = new XmlPopulatorContext(cmdContext, fieldNames);
		try (ModeCloser seqMode = cmdContext.pushCommandMode("sequence", sourceName, sequenceID)) {
			rules.forEach(rule -> {
				if(format.equals(SequenceFormat.GENBANK_XML.name())) {
					OriginalDataResult originalDataResult = 
							cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
					Document sequenceDataDoc;
					try {
						sequenceDataDoc = GlueXmlUtils.documentFromBytes(originalDataResult.getBase64Bytes());
					} catch (Exception e) {
						throw new RuntimeException("Bad GENBANK XML format: "+e.getMessage(), e);
					}
					rule.execute(xmlPopulatorContext, sequenceDataDoc);
				}
			});
		}
		xmlPopulatorContext.getFieldUpdates().forEach((fieldName, value) -> {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put(Sequence.SOURCE_NAME_PATH, sourceName);
			row.put(Sequence.SEQUENCE_ID_PROPERTY, sequenceID);
			row.put("fieldName", fieldName);
			row.put("value", value);
			rowData.add(row);
		});
		
	}
	
	private PopulateResult populate(CommandContext cmdContext, int batchSize, 
			Optional<Expression> whereClause, boolean updateDB, List<String> fieldNames) {
		log("Finding sequences to process");
		CommandBuilder<ListResult, ListSequenceCommand> cmdBuilder = cmdContext.cmdBuilder(ListSequenceCommand.class);
		whereClause.ifPresent(wc ->
			cmdBuilder.set(ListSequenceCommand.WHERE_CLAUSE, wc.toString())
		);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.SOURCE_NAME_PATH);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.SEQUENCE_ID_PROPERTY);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.FORMAT_PROPERTY);
		ListResult listResult = cmdBuilder.execute();
		List<Map<String,Object>> sequenceMaps = listResult.asListOfMaps();
		List<Map<String,Object>> rowData = new LinkedList<Map<String, Object>>();
		log("Found "+sequenceMaps.size()+" sequences to process");
		int sequencesProcessed = 0;
		for(Map<String,Object> sequenceMap: sequenceMaps) {
			String sourceName = (String) sequenceMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) sequenceMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			String format = (String) sequenceMap.get(Sequence.FORMAT_PROPERTY);
			populate(cmdContext, sourceName, sequenceID, format, rowData, fieldNames);
			sequencesProcessed++;
			if(sequencesProcessed % batchSize == 0) {
				log("Processed "+sequencesProcessed+" sequences");
				if(updateDB) {
					cmdContext.commit();
				} 
				cmdContext.newObjectContext();
			}
		}
		log("Processed "+sequencesProcessed+" sequences");
		if(updateDB) {
			cmdContext.commit();
		}
		cmdContext.newObjectContext();
		return new PopulateResult(rowData);
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
				registerVariableInstantiator("fieldName", new ModifiableFieldNameInstantiator(ConfigurableTable.sequence));
			}
		}
		
	}

	
}
