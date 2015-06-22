package uk.ac.gla.cvr.gluetools.core;

import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterException;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterException.Code;

public class TestException {

	@Test
	public void test() throws Exception {
		try {
			throw new NcbiImporterException(Code.IO_ERROR, "fooo", "bar");
		} catch(NcbiImporterException sse) {
			System.out.println(sse.getMessage());
		}
	}
}
