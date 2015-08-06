package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@PluginClass(elemName="genbankXmlPopulator")
public class GenbankXmlPopulatorPlugin extends SequencePopulatorPlugin<GenbankXmlPopulatorPlugin> {

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
					Element showDataElem = CommandUsage.docElemForCmdClass(ShowOriginalDataCommand.class);
					OriginalDataResult originalDataResult = (OriginalDataResult) cmdContext.executeElem(showDataElem.getOwnerDocument().getDocumentElement());
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
	
	private CommandResult populate(CommandContext cmdContext) {
		Element listSequencesElem = CommandUsage.docElemForCmdClass(ListSequenceCommand.class);
		getWhereClause().ifPresent(wc ->
			GlueXmlUtils.appendElementWithText(listSequencesElem, ListSequenceCommand.WHERE_CLAUSE, wc.toString())
		);
		GlueXmlUtils.appendElementWithText(listSequencesElem, 
				ListSequenceCommand.FIELD_NAME, Sequence.SOURCE_NAME_PATH);
		GlueXmlUtils.appendElementWithText(listSequencesElem, 
				ListSequenceCommand.FIELD_NAME, Sequence.SEQUENCE_ID_PROPERTY);
		GlueXmlUtils.appendElementWithText(listSequencesElem, 
				ListSequenceCommand.FIELD_NAME, Sequence.FORMAT_PROPERTY);
		ListResult listResult = (ListResult) cmdContext.executeElem(listSequencesElem.getOwnerDocument().getDocumentElement());
		List<Map<String,String>> sequenceMaps = listResult.asListOfMaps();
		
		for(Map<String,String> sequenceMap: sequenceMaps) {
			String sourceName = sequenceMap.get(Sequence.SOURCE_NAME_PATH);
			String sequenceID = sequenceMap.get(Sequence.SEQUENCE_ID_PROPERTY);
			String format = sequenceMap.get(Sequence.FORMAT_PROPERTY);
			populate(cmdContext, sourceName, sequenceID, format);
		}
		return CommandResult.OK;
	}
	

	@CommandClass( 
			commandWords={"populate"}, 
			docoptUsages={""},
			description="Populate sequence field values based on Genbank XML") 
	public static class PopulateCommand extends ModuleProvidedCommand<GenbankXmlPopulatorPlugin> implements ProvidedProjectModeCommand {
		
		@Override
		protected CommandResult execute(CommandContext cmdContext, GenbankXmlPopulatorPlugin populatorPlugin) {
			return populatorPlugin.populate(cmdContext);
		}
		
	}

	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this populator") 
	public static class ShowPopulatorCommand extends ShowConfigCommand<GenbankXmlPopulatorPlugin> {}
	
	
	@SimpleConfigureCommandClass(
			propertyNames={"whereClause"}
	)
	public static class ConfigurePopulatorCommand extends SimpleConfigureCommand<GenbankXmlPopulatorPlugin> {}

	
}
