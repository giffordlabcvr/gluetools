package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.test;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignedSegment;

public abstract class TestBlast {

	protected BlastAlignedSegment segment(int refStart, int refEnd, int queryStart, int queryEnd) {
		return new BlastAlignedSegment(refStart, refEnd, queryStart, queryEnd, null);
	}

}
