package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;
import gnu.trove.map.hash.TIntIntHashMap;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.BaseSamReporterCommand.SamRefInfo;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class SamExportNucleotideAlignmentCommandContext extends SamBaseNucleotideCommandContext {

	private Map<String, char[]> readNameToAlmtRow = new LinkedHashMap<String, char[]>();
	private TIntIntHashMap relRefNtToAlmtRowNt = new TIntIntHashMap();
	private int almtRowWidth;
	
	public SamExportNucleotideAlignmentCommandContext(SamReporter samReporter, SamRefInfo samRefInfo,
			List<QueryAlignedSegment> samRefToRelatedRefSegs, SamRefSense samRefSense,
			List<ReferenceSegment> selectedRefSegs, 
			SamRecordFilter samRecordFilter) {
		super(samReporter, samRefInfo, samRefToRelatedRefSegs, selectedRefSegs, samRefSense, samRecordFilter);
		int almtRowNt = 0;
		for(ReferenceSegment refSeg: selectedRefSegs) {
			for(int i = refSeg.getRefStart(); i <= refSeg.getRefEnd(); i++) {
				this.relRefNtToAlmtRowNt.put(i, almtRowNt);
				almtRowNt++;
			}
		}
		this.almtRowWidth = almtRowNt;
	}

	public char[] getAlmtRow(String readName) {
		char[] almtRow = readNameToAlmtRow.get(readName);
		if(almtRow == null) {
			almtRow = new char[almtRowWidth];
			for(int i = 0; i < almtRowWidth; i++) {
				almtRow[i] = '-';
			}
			readNameToAlmtRow.put(readName, almtRow);
		}
		return almtRow;
	}

	public void processReadBase(String readName, int relatedRefNt, char base) {
		getAlmtRow(readName)[relRefNtToAlmtRowNt.get(relatedRefNt)] = base;
	}
	
	public Map<String, DNASequence> getFastaMap() {
		Map<String, DNASequence> fastaMap = new LinkedHashMap<String, DNASequence>();
		readNameToAlmtRow.forEach((readName, almtRow) -> {
			fastaMap.put(readName, FastaUtils.ntStringToSequence(new String(almtRow)));
		});
		return fastaMap;
	}
	
}
