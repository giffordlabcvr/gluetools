package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@PluginClass(elemName="genbankXmlPopulator")
public class GenbankXmlPopulator extends SequencePopulator<GenbankXmlPopulator> {

	private List<XmlPopulatorRule> rules;
	
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
		addProvidedCmdClass(PopulateCommand.class);
		addProvidedCmdClass(ShowPopulatorCommand.class);
		addProvidedCmdClass(ConfigurePopulatorCommand.class);
	}

	private void populate(CommandContext cmdContext, String sourceName, String sequenceID, String format) {
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
					rule.execute(cmdContext, sequenceDataDoc);
				}
			});
		} 
	}
	
	private OkResult populate(CommandContext cmdContext, int batchSize) {
		GlueLogger.getGlueLogger().fine("Finding sequences to process");
		CommandBuilder<ListResult, ListSequenceCommand> cmdBuilder = cmdContext.cmdBuilder(ListSequenceCommand.class);
		getWhereClause().ifPresent(wc ->
			cmdBuilder.set(ListSequenceCommand.WHERE_CLAUSE, wc.toString())
		);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.SOURCE_NAME_PATH);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.SEQUENCE_ID_PROPERTY);
		cmdBuilder.set(ListSequenceCommand.FIELD_NAME, Sequence.FORMAT_PROPERTY);
		ListResult listResult = cmdBuilder.execute();
		List<Map<String,Object>> sequenceMaps = listResult.asListOfMaps();
		GlueLogger.getGlueLogger().fine("Found "+sequenceMaps.size()+" sequences to process");
		int sequencesProcessed = 0;
		for(Map<String,Object> sequenceMap: sequenceMaps) {
			String sourceName = (String) sequenceMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = (String) sequenceMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			String format = (String) sequenceMap.get(Sequence.FORMAT_PROPERTY);
			populate(cmdContext, sourceName, sequenceID, format);
			sequencesProcessed++;
			if(sequencesProcessed % batchSize == 0) {
				GlueLogger.getGlueLogger().fine("Processed "+sequencesProcessed+" sequences");
				cmdContext.commit();
				cmdContext.newObjectContext();
			}
		}
		GlueLogger.getGlueLogger().fine("Processed "+sequencesProcessed+" sequences");
		cmdContext.commit();
		return CommandResult.OK;
	}
	

	@CommandClass( 
			commandWords={"populate"}, 
			docoptUsages={"[-b <batchSize>]"},
			docoptOptions={
					"-b <batchSize>, --batchSize <batchSize>  Commit batch size [default: 250]"
			},
			metaTags={CmdMeta.updatesDatabase},
			description="Populate sequence field values based on Genbank XML",
			furtherHelp=
			"The <batchSize> argument allows you to control how often updates are committed to the database "+
					"during the import. The default is every 250 sequences. A larger <batchSize> means fewer database "+
					"accesses, but requires more Java heap memory.") 
	public static class PopulateCommand extends ModuleProvidedCommand<OkResult, GenbankXmlPopulator> implements ProvidedProjectModeCommand {
		
		private Integer batchSize;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
		}

		@Override
		protected OkResult execute(CommandContext cmdContext, GenbankXmlPopulator populatorPlugin) {
			return populatorPlugin.populate(cmdContext, batchSize);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {}
		
	}

	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this populator") 
	public static class ShowPopulatorCommand extends ShowConfigCommand<GenbankXmlPopulator> {}
	
	
	@SimpleConfigureCommandClass(
			propertyNames={"whereClause"}
	)
	public static class ConfigurePopulatorCommand extends SimpleConfigureCommand<GenbankXmlPopulator> {}

	
}
