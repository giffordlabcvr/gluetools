package uk.ac.gla.cvr.gluetools.core;

import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ImporterPluginException;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ImporterPluginException.Code;

public class TestException {

	@Test
	public void test() throws Exception {
		try {
			throw new ImporterPluginException(Code.IO_ERROR, "fooo", "bar");
		} catch(ImporterPluginException sse) {
			System.out.println(sse.getMessage());
		}
	}
}
