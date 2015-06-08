package uk.ac.gla.cvr.gluetools.core;

import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerException;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerException.Code;

public class TestException {

	@Test
	public void test() throws Exception {
		try {
			throw new SequenceSourcerException(Code.IO_ERROR, "fooo", "bar");
		} catch(SequenceSourcerException sse) {
			System.out.println(sse.getMessage());
		}
	}
}
