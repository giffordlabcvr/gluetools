package uk.ac.gla.cvr.gluetools.core.collation.genbank.ncbi;

import org.junit.Test;

public class TestNcbiGenbank {

	@Test
	public void test() throws Exception {
		NcbiGenbankCollatedSequenceSource source = new NcbiGenbankCollatedSequenceSource();
		
		source.updateSequences();
	}
	
}
