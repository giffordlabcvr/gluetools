package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


@CommandClass( 
	commandWords={"show", "sequence"}, 
	docoptUsages={"[-s <sourceName>] <sequenceID>"},
	docoptOptions={"-s <sourceName>, --sourceName <sourceName>  Specify a particular source"},
	description="Show the data for a sequence") 
public class ShowSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}


	// TODO sort out exceptions here.
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext, sourceName, sequenceID, false);
		if(sequence.getFormat().equals(SequenceFormat.GENBANK_XML.name())) {
			try {
				return new DocumentResult(XmlUtils.documentFromBytes(sequence.getData()));
			} catch (SAXException e) {
				throw new RuntimeException("Sequence data is malformed XML");
			}
		} else {
			throw new RuntimeException("Sequence format is not XML-based");
		}
	}


}
