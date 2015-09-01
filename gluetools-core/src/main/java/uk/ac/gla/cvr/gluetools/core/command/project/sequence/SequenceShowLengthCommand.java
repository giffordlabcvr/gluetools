package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;


@CommandClass( 
	commandWords={"show", "length"}, 
	docoptUsages={""},
	description="Show the length of the sequence in nucleotides") 
public class SequenceShowLengthCommand extends SequenceModeCommand<SequenceShowLengthCommand.SequenceShowLengthResult> {

	@Override
	public SequenceShowLengthResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		return new SequenceShowLengthResult(sequence.getSequenceObject().getNucleotides().length());
	}

	public static class SequenceShowLengthResult extends MapResult {

		public SequenceShowLengthResult(int length) {
			super("lengthResult", mapBuilder().put("length", length));
		}
		
		public Integer getLength() {
			return getDocumentReader().intValue("length");
		}

	}

}
