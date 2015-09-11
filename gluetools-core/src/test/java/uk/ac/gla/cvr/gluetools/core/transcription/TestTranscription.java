package uk.ac.gla.cvr.gluetools.core.transcription;

import org.junit.Assert;
import org.junit.Test;


public class TestTranscription {

	
	/*
	
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

	*/

	@Test
	public void testTranscription1() {
		transcriptionTest("", ""); 
	}

	@Test
	public void testTranscription2() {
		transcriptionTest("ATGGGGCCCTAG", "MGP*"); 
	}

	@Test
	public void testTranscription3() {
		transcriptionTest("ATCTAG", ""); // not a start codon.
	}

	@Test
	public void testTranscription4() {
		transcriptionTest("ATGGGGCCCTAGCCCCCC", "MGP*"); 
	}

	@Test
	public void testTranscription5() {
		transcriptionTest("ATGGGGNNNGGCC", "MG"); // NNN could be a stop codon 
	}

	@Test
	public void testTranscription6() {
		transcriptionTest("A", ""); 
	}

	@Test
	public void testTranscription7() {
		transcriptionTest("AT", ""); 
	}

	@Test
	public void testTranscription8() {
		transcriptionTest("ATGGGGCCC-CCCTAG", "MGP"); // gap 
	}

	@Test
	public void testTranscription9() {
		transcriptionTest("ATGGGGCCCC-CCTAG", "MGP"); // gap 
	}

	@Test
	public void testTranscription10() {
		transcriptionTest("ATGGGGCCCCC-CTAG", "MGPP"); // gap 
	}

	private static void transcriptionTest(String nucleotides, String expectedAas) {
		Assert.assertEquals(expectedAas, TranscriptionUtils.transcribe(nucleotides));
	}
		

}