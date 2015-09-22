package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

// Analysis results for a single sequence.
public class SequenceResult {
	private String sourceName;
	private String sequenceID;
	private String initialAlignmentName;
	private AbstractSequenceObject seqObj;
	private int sequenceLength;

	
	// this list indicates the analysis of the sequence for a path of alignments
	// through the alignment tree.
	// in future there might be different starting points for different features, e.g. in the case of 
	// recombinants or segmented genomes.
	List<SequenceAlignmentResult> seqAlignmentResults = new ArrayList<SequenceAlignmentResult>();
	
	public SequenceResult(CommandContext cmdContext, String sourceName, String sequenceID,
			String initialAlignmentName, AbstractSequenceObject seqObj) {
		super();
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
		this.initialAlignmentName = initialAlignmentName;
		this.seqObj = seqObj;
		this.sequenceLength = seqObj.getNucleotides(cmdContext).length();
	}

	public void toDocument(ObjectBuilder seqResultObj) {
		seqResultObj.setString("sourceName", sourceName);
		seqResultObj.setString("sequenceID", sequenceID);
		seqResultObj.setString("initialAlignmentName", initialAlignmentName);
		ArrayBuilder seqAlmtResultArray = seqResultObj.setArray("sequenceAlignmentResult");
		for(SequenceAlignmentResult almtAnalysis: seqAlignmentResults) {
			almtAnalysis.toDocument(seqAlmtResultArray.addObject());
		}
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public String getInitialAlignmentName() {
		return initialAlignmentName;
	}

	public AbstractSequenceObject getSeqObj() {
		return seqObj;
	}

	public int getSequenceLength() {
		return sequenceLength;
	}

	public List<SequenceAlignmentResult> getSeqAlignmentResults() {
		return seqAlignmentResults;
	}

	/**
	 * Set up the chain of SequenceAlignmentResult objects for a specific sequence result.
	 * The start of this chain is the initial alignment, the chain then follows that alignment's ancestors.
	 */
	public void initSeqAlmtResults(List<Map<String, Object>> ancestorsListOfMaps) {
		for(Map<String, Object> ancestorMap: ancestorsListOfMaps) {
			SequenceAlignmentResult currentSeqAlmtResult = new SequenceAlignmentResult(
					(String) ancestorMap.get(Alignment.NAME_PROPERTY), 
					(String) ancestorMap.get(Alignment.REF_SEQ_NAME_PATH)
					);
			seqAlignmentResults.add(currentSeqAlmtResult);
		}
	}


	
	
	
}