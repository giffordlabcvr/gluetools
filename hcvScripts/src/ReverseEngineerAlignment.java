

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class ReverseEngineerAlignment {

	private static class AlignedSeg {
		public AlignedSeg(int alignedStart, int alignedEnd, int unalignedStart,
				int unalignedEnd) {
			super();
			this.alignedStart = alignedStart;
			this.alignedEnd = alignedEnd;
			this.unalignedStart = unalignedStart;
			this.unalignedEnd = unalignedEnd;
		}
		int alignedStart;
    	int alignedEnd;
    	int unalignedStart;
    	int unalignedEnd;
	}
	
	private static final String H77_ACC = "AF009606";
	
	public static void main(String[] args) throws Exception {

		String directory = "/Users/joshsinger/gitrepos/gluetools/gluetools-core/hcvProject";
		String alignedFastaFile = "Donald_Smith_HCV_alignment_May_26_2015.filtered.fasta";
		String unalignedFastaFile = "unaligned.fasta";
		
		// may need to update pattern to extract genotype string from FASTA seq id
		Pattern pattern = Pattern.compile("(\\d[a-z]*)\\??_.*");
	    
	    Map<String, DNASequence> alignedSeqMapWithGenotypes;
	    try(FileInputStream fis = new FileInputStream(new File(directory, alignedFastaFile))) {
	    	alignedSeqMapWithGenotypes = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }

	    final Map<String, DNASequence> alignedSeqMap = new LinkedHashMap<String, DNASequence>();
	    final Map<String, DNASequence> unalignedSeqMap;

	    try(FileInputStream fis = new FileInputStream(new File(directory, unalignedFastaFile))) {
	    	unalignedSeqMap = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }

	    // strip out the genotype data.
	    alignedSeqMapWithGenotypes.forEach((fastaID, dnaSequence) -> {
	    	Matcher matcher = pattern.matcher(fastaID);
	    	matcher.find();
	    	String primaryAcc = fastaID.replaceAll(".*_", "");
	    	alignedSeqMap.put(primaryAcc, dnaSequence);
	    });

	    // map of accession number to list of segments relating the aligned and unaligned for that accession.
	    Map<String, List<AlignedSeg>> accToAlignedSegs = new LinkedHashMap<String, List<AlignedSeg>>();
	    
	    alignedSeqMap.keySet().forEach(primaryAcc -> {
	    	String unaligned = unalignedSeqMap.get(primaryAcc).toString();
	    	String aligned = alignedSeqMap.get(primaryAcc).toString();
	    	accToAlignedSegs.put(primaryAcc, findAlignedSegs(unaligned, aligned));
	    });
	    
	    List<AlignedSeg> refSegs = accToAlignedSegs.get(H77_ACC);
	    accToAlignedSegs.forEach((memberAcc, memberSegs) -> {
	    	System.out.println("    member -w \"GB_PRIMARY_ACCESSION = '"+memberAcc+"'\"");
	    	for(AlignedSeg refSeg: refSegs) {
	    		for(AlignedSeg memberSeg: memberSegs) {
	    			generateMemberSeg(refSeg, memberSeg);
	    		}
	    	}
	    	System.out.println("      exit");
	    	
	    });
	    
	}

	private static void generateMemberSeg(AlignedSeg refSeg, AlignedSeg memberSeg) {
		if(memberSeg.alignedStart > refSeg.alignedEnd || 
				refSeg.alignedStart > memberSeg.alignedEnd) {
			return; // no overlap
		}
		// overlapping part, in alignment coordinates.
		int alnOverlapStart = Math.max(memberSeg.alignedStart, refSeg.alignedStart);
		int alnOverlapEnd = Math.min(memberSeg.alignedEnd, refSeg.alignedEnd);
		// translate overlap region to ref coordinates
		int refOffset = refSeg.unalignedStart-refSeg.alignedStart;
		int refStart = alnOverlapStart + refOffset; 
		int refEnd = alnOverlapEnd + refOffset; 
		// translate overlap region to member coordinates
		int memberOffset = memberSeg.unalignedStart-memberSeg.alignedStart;
		int memberStart = alnOverlapStart + memberOffset; 
		int memberEnd = alnOverlapEnd + memberOffset;
		System.out.println("      add segment "+refStart+" "+refEnd+" "+memberStart+" "+memberEnd);
	}

	public static List<AlignedSeg> findAlignedSegs(String unaligned,
			String aligned) {
		List<AlignedSeg> alignedSegs = new ArrayList<AlignedSeg>();
    	
    	int alignedStart = 1;
    	int alignedEnd;
    	int unalignedStart = 1;
    	int unalignedEnd;
    	while(alignedStart <= aligned.length()) {
    		while(alignedStart <= aligned.length() && nt(aligned, alignedStart).matches("[N-]")) {
    			alignedStart++;
    		}
    		if(alignedStart <= aligned.length()) {
    			alignedEnd = alignedStart;
    			while(alignedEnd < aligned.length() && !nt(aligned, alignedEnd+1).matches("[N-]")) {
    				alignedEnd++;
    			}
    			String alignedSubSeq = subSeq(aligned, alignedStart, alignedEnd);
    			unalignedStart = find(alignedSubSeq, unaligned, unalignedStart);
    			if(unalignedStart == -1) {
    				throw new RuntimeException("Subsequence "+alignedSubSeq+" not found in unaligned sequence");
    			}
    			unalignedEnd = unalignedStart+alignedSubSeq.length()-1;
    			alignedSegs.add(new AlignedSeg(alignedStart, alignedEnd, unalignedStart, unalignedEnd));
    			unalignedStart = unalignedEnd+1;
    			alignedStart = alignedEnd+1;
    			
    		}
    	}
		return alignedSegs;
	}

	private static String nt(String seq, int position) {
		return seq.substring(position-1, position);
	}

	private static String subSeq(String seq, int start, int end) {
		return seq.substring(start-1, end);
	}
	
	private static int find(String query, String sequence, int from) {
		int index = sequence.indexOf(query, from-1);
		if(index == -1) { return index; }
		return index+1;
	}
}
