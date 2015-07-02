package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

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
		String alternateElemsXPath = XmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		rules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
		addProvidedCmdClass(PopulateCommand.class);
		addProvidedCmdClass(ShowPopulatorCommand.class);
		addProvidedCmdClass(ConfigurePopulatorCommand.class);
	}

	private void populate(CommandContext cmdContext, Sequence sequence) {
		ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
		cmdContext.pushCommandMode(new SequenceMode(projectMode.getProject(), sequence.getSource().getName(), sequence.getSequenceID()));
		try {
			rules.forEach(rule -> {
				if(sequence.getFormat().equals(SequenceFormat.GENBANK_XML.name())) {
					Document sequenceDataDoc = null; 
					try {
						sequenceDataDoc = XmlUtils.documentFromBytes(sequence.getData());
					} catch (SAXException se) {
						throw new XmlPopulatorException(se, Code.SEQUENCE_INCORRECTLY_FORMATTED, sequence.getObjectId().getIdSnapshot(), se.getLocalizedMessage());
					}
					rule.execute(cmdContext, sequenceDataDoc);
				}
			});
		} finally {
			cmdContext.popCommandMode();
		}
	}
	
	private CommandResult populate(CommandContext cmdContext) {
		Element listSequencesElem = CommandUsage.docElemForCmdClass(ListSequencesCommand.class);
		getWhereClause().ifPresent(wc ->
			XmlUtils.appendElementWithText(listSequencesElem, ListSequencesCommand.WHERE_CLAUSE, wc.toString())
		);
		@SuppressWarnings("unchecked")
		ListCommandResult<Sequence> listResult = (ListCommandResult<Sequence>) cmdContext.executeElem(listSequencesElem.getOwnerDocument().getDocumentElement());
		
		for(Sequence sequence: listResult.getResults()) {
			populate(cmdContext, sequence);
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
