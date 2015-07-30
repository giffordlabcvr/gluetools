package uk.ac.gla.cvr.gluetools.core.collation.importing;

import java.util.Base64;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class ImporterPlugin<P extends ImporterPlugin<P>> extends ModulePlugin<P> {

	protected final void ensureSourceExists(CommandContext cmdContext, String sourceName) {
		Element createSourceElem = CommandUsage.docElemForCmdClass(CreateSourceCommand.class);
		GlueXmlUtils.appendElementWithText(createSourceElem, CreateSourceCommand.SOURCE_NAME, sourceName);
		GlueXmlUtils.appendElementWithText(createSourceElem, CreateSourceCommand.ALLOW_EXISTING, "true");
		cmdContext.executeElem(createSourceElem.getOwnerDocument().getDocumentElement());
	}
	
	protected final void createSequence(CommandContext cmdContext, String sourceName, String sequenceID, 
			SequenceFormat format, byte[] sequenceData) {
		Element createSeqElem = CommandUsage.docElemForCmdClass(CreateSequenceCommand.class);
		GlueXmlUtils.appendElementWithText(createSeqElem, CreateSequenceCommand.SOURCE_NAME, sourceName);
		GlueXmlUtils.appendElementWithText(createSeqElem, CreateSequenceCommand.SEQUENCE_ID, sequenceID);
		GlueXmlUtils.appendElementWithText(createSeqElem, CreateSequenceCommand.FORMAT, format.name());
		// character encoding presumably not important here.
		String base64String = new String(Base64.getEncoder().encode(sequenceData));
		GlueXmlUtils.appendElementWithText(createSeqElem, CreateSequenceCommand.BASE64, base64String);
		cmdContext.executeElem(createSeqElem.getOwnerDocument().getDocumentElement());
	}
}
