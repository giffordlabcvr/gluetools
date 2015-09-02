package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "sequence"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the source/sequenceID sequence"
)
public class ReferenceShowSequenceCommand extends ReferenceSequenceModeCommand<ReferenceShowSequenceCommand.ReferenceShowSequenceResult> {

	
	@Override
	public ReferenceShowSequenceResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				ReferenceSequence.class, ReferenceSequence.pkMap(getRefSeqName()), false);
		return new ReferenceShowSequenceResult(refSeq.getSequence().getSource().getName(), refSeq.getSequence().getSequenceID());
	}

	public static class ReferenceShowSequenceResult extends MapResult {

		public ReferenceShowSequenceResult(String sourceName, String sequenceID) {
			super("showSequenceResult", mapBuilder()
				.put(ReferenceSequence.SEQ_SOURCE_NAME_PATH, sourceName)
				.put(ReferenceSequence.SEQ_ID_PATH, sequenceID));
		}

		public String getSourceName() {
			return getDocumentReader().stringValue(ReferenceSequence.SEQ_SOURCE_NAME_PATH);
		}

		public String getSequenceID() {
			return getDocumentReader().stringValue(ReferenceSequence.SEQ_ID_PATH);
		}

		
	}

}
