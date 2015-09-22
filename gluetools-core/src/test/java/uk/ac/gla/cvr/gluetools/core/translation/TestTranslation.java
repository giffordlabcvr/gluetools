package uk.ac.gla.cvr.gluetools.core.translation;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;


public class TestTranslation {

	
	/*
	
	@Test
	public void testTranslation1() {
		translationTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 210, "AUUUGGGUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranslation2() {
		translationTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 206, "AUUUGG"),
						ntSeg(207, 210, "GUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranslation3() {
		translationTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 206, "AUUUGG"),
						ntSeg(208, 210, "UUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	@Test
	public void testTranslation4() {
		translationTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(201, 205, "AUUUG"),
						ntSeg(206, 210, "GGUUU")
				), 
				expectedAaSegments(
						"Ref: [2, 4] AAs: FGF"
				));
	}

	
	@Test
	public void testTranslation5() {
		translationTest(199, // NT location for codon 1 
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
	public void testTranslation6() {
		translationTest(199, // NT location for codon 1 
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
	public void testTranslation7() {
		translationTest(198, // NT location for codon 1 
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
	public void testTranslation8() {
		translationTest(199, // NT location for codon 1 
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
	public void testTranslation9() {
		translationTest(199, // NT location for codon 1 
				ntSegs(
						ntSeg(261, 266, "NGGGGC")
				), 
				expectedAaSegments(
						"Ref: [22, 23] AAs: GA"
				));
	}

	@Test
	public void testTranslation10() {
		translationTest(199, // NT location for codon 1 
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
	public void testTranslation1() {
		translationTest("", ""); 
	}

	@Test
	public void testTranslation2() {
		translationTest("ATGGGGCCCTAG", "MGP*"); 
	}

	@Test
	public void testTranslation3() {
		translationTest("ATCTAG", ""); // not a start codon.
	}

	@Test
	public void testTranslation4() {
		translationTest("ATGGGGCCCTAGCCCCCC", "MGP*"); 
	}

	@Test
	public void testTranslation5() {
		translationTest("ATGGGGNNNGGCC", "MG"); // NNN could be a stop codon 
	}

	@Test
	public void testTranslation6() {
		translationTest("A", ""); 
	}

	@Test
	public void testTranslation7() {
		translationTest("AT", ""); 
	}

	@Test
	public void testTranslation8() {
		translationTest("ATGGGGCCC-CCCTAG", "MGP"); // gap 
	}

	@Test
	public void testTranslation9() {
		translationTest("ATGGGGCCCC-CCTAG", "MGP"); // gap 
	}

	@Test
	public void testTranslation10() {
		translationTest("ATGGGGCCCCC-CTAG", "MGPP"); // gap 
	}

	private static void translationTest(String nucleotides, String expectedAas) {
		Assert.assertEquals(expectedAas, TranslationUtils.translate(nucleotides, true, true, false));
	}
		

}