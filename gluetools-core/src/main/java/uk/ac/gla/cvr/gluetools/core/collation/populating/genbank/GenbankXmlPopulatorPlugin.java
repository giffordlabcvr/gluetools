package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

// TODO remove legacy populator stuff.
@PluginClass(elemName="genbankXmlPopulator")
public class GenbankXmlPopulatorPlugin implements ModulePlugin {

	private List<XmlPopulatorRule> rules;
	
	protected List<XmlPopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		XmlPopulatorRuleFactory populatorRuleFactory = PluginFactory.get(GenbankXmlPopulatorRuleFactory.creator);
		String alternateElemsXPath = XmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		rules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
	}

	private void populate(CommandContext cmdContext, Sequence sequence) {
		rules.forEach(rule -> {
			if(!sequence.getFormat().equals(SequenceFormat.GENBANK_XML.name())) {
				throw new XmlPopulatorException(Code.INCOMPATIBLE_SEQUENCE_FORMAT, sequence.getObjectId().getIdSnapshot(), sequence.getFormat());
			}
			Document sequenceDataDoc = null; 
			try {
				sequenceDataDoc = XmlUtils.documentFromBytes(sequence.getData());
			} catch (SAXException se) {
				throw new XmlPopulatorException(se, Code.SEQUENCE_INCORRECTLY_FORMATTED, sequence.getObjectId().getIdSnapshot(), se.getLocalizedMessage());
			}
			rule.execute(cmdContext, sequence.getSource().getName(), sequence.getSequenceID(), sequenceDataDoc);
		});
	}

	
	@Override
	public CommandResult runModule(CommandContext cmdContext) {
		String sourceName = "ncbi-nuccore";
		Element listSequencesElem = CommandUsage.docElemForCmdClass(ListSequencesCommand.class);
		XmlUtils.appendElementWithText(listSequencesElem, "sourceName", sourceName);
		@SuppressWarnings("unchecked")
		ListCommandResult<Sequence> listResult = (ListCommandResult<Sequence>) cmdContext.executeElem(listSequencesElem.getOwnerDocument().getDocumentElement());
		
		for(Sequence sequence: listResult.getResults()) {
			populate(cmdContext, sequence);
		}
		return CommandResult.OK;
	}
	
	
}
