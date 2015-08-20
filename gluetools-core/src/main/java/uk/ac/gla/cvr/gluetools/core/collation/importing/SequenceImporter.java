package uk.ac.gla.cvr.gluetools.core.collation.importing;

import java.util.Base64;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequenceImporter<P extends SequenceImporter<P>> extends ModulePlugin<P> {

	protected final void ensureSourceExists(CommandContext cmdContext, String sourceName) {
		cmdContext.cmdBuilder(CreateSourceCommand.class).
			set(CreateSourceCommand.SOURCE_NAME, sourceName).
			set(CreateSourceCommand.ALLOW_EXISTING, "true").
			execute();
	}
	
	protected final void createSequence(CommandContext cmdContext, String sourceName, String sequenceID, 
			SequenceFormat format, byte[] sequenceData) {
		cmdContext.cmdBuilder(CreateSequenceCommand.class).
			set(CreateSequenceCommand.SOURCE_NAME, sourceName).
			set(CreateSequenceCommand.SEQUENCE_ID, sequenceID).
			set(CreateSequenceCommand.FORMAT, format.name()).
			set(CreateSequenceCommand.ORIGINAL_DATA, new String(Base64.getEncoder().encode(sequenceData))).
			execute();
	}
}
