package uk.ac.gla.cvr.gluetools.core;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerFactoryException;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerFactoryException.Code;

public class TestException {

	
	@Test
	public void testExceptionMessage() {
		String exceptionMessage;
		try {
			throw new SequenceSourcerFactoryException(Code.UNKNOWN_SOURCER_TYPE, "foo");
		} catch(GlueException ge) {
			exceptionMessage = ge.getMessage();
		}
		Assert.assertEquals("No SequenceSourcer of type foo is known.", exceptionMessage);
	}
	
}
