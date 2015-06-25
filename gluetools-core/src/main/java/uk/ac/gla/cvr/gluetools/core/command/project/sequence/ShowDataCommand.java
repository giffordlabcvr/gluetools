package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


@CommandClass( 
	commandWords={"show", "data"}, 
	docoptUsages={""},
	docoptOptions={""},
	description="Show the sequence data") 
public class ShowDataCommand extends SequenceModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
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
