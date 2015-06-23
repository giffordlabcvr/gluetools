package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@PluginClass(elemName="show-sequence")
@CommandClass(description="Show the data for a sequence", 
	docoptUsages={"<sourceName> <sequenceID>"}) 
public class ShowSequenceCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}


	// TODO sort out exceptions here.
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = GlueDataObject.lookup(objContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
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
