package uk.ac.gla.cvr.gluetools.core.transcription;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;

public class TestTranscription {

	

	
	@Test
	public void testTranscription1() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 210, "AUUUGGGUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranscription2() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 206, "AUUUGG"),
						ntSeg(207, 210, "GUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranscription3() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 206, "AUUUGG"),
						ntSeg(208, 210, "UUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranscription4() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 205, "AUUUG"),
						ntSeg(206, 210, "GGUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	
	@Test
	public void testTranscription5() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 208, "AUUUAAAG"),
						ntSeg(261, 265, "NGGGN")
				), 
				expectedAaSegments(
						"Ref: [2, 3] AAs: FK",
						"Ref: [22, 22] AAs: G"
				));
	}

	@Test
	public void testTranscription6() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(262, 291, "UUUGGGUUUGGGUUUGGGUUUGGGUUUGGG"),
						ntSeg(295, 309, "AAACCCAAACCCAAA")
				), 
				expectedAaSegments(
						"Ref: [22, 31] AAs: FGFGFGFGFG",
						"Ref: [33, 37] AAs: KPKPK"
				));
	}

	@Test
	public void testTranscription7() {
		transcriptionTest(198, // NT location for codon 1 
				ntSegs(
						ntSeg(262, 291, "UUUGGGUUUGGGUUUGGGUUUGGGUUUGGG"),
						ntSeg(295, 309, "AAACCCAAACCCAAA")
				), 
				expectedAaSegments(
						"Ref: [23, 31] AAs: WVWVWVWVW",
						"Ref: [34, 37] AAs: TQTQ"
				));
	}
	
	@Test
	public void testTranscription8() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 210, "AUUUAAAGNN"),
						ntSeg(261, 265, "NGGGN")
				), 
				expectedAaSegments(
						"Ref: [2, 3] AAs: FK",
						"Ref: [22, 22] AAs: G"
				));
	}

	@Test
	public void testTranscription9() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(261, 266, "NGGGGC")
				), 
				expectedAaSegments(
						"Ref: [22, 23] AAs: GA"
				));
	}

	@Test
	public void testTranscription10() {
		transcriptionTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(261, 262, "NG"),
						ntSeg(263, 263, "A"),
						ntSeg(264, 266, "ANN")
				), 
				expectedAaSegments(
						"Ref: [22, 22] AAs: E"
				));
	}

	
	
	
	
	
	
	private static void transcriptionTest(int codon1Start, NtReferenceSegment[] ntRefSegments, String[] expectedAaSegments) {
		Assert.assertEquals(Arrays.asList(expectedAaSegments), 
				TranscriptionUtils.transcribeAminoAcids(codon1Start, Arrays.asList(ntRefSegments))
				.stream()
				.map(AaReferenceSegment::toString)
				.collect(Collectors.toList()));
		
	}
	private static NtReferenceSegment ntSeg(int refStart, int refEnd, String nucleotides) {
		NtReferenceSegment ntSeg = new NtReferenceSegment(refStart, refEnd, nucleotides);
		if(nucleotides.length() != ntSeg.getCurrentLength()) {
			throw new RuntimeException("ntSeg nucleotides of incorrect length");
		}
		return ntSeg;
		
	}
	
	private static NtReferenceSegment[] ntSegs(NtReferenceSegment ... ntSegs) {
		return ntSegs;
	}

	private static String[] expectedAaSegments(String ... expectedAaSegments) {
		return expectedAaSegments;
	}

	
}
