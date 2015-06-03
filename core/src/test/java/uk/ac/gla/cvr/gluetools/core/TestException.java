package uk.ac.gla.cvr.gluetools.core;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;

public class TestException {

	
	@Test
	public void testExceptionMessage() {
		String exceptionMessage;
		try {
			throw new PluginFactoryException(Code.INCORRECT_ROOT_ELEMENT, "SomeFactory", "someRootElemName");
		} catch(GlueException ge) {
			exceptionMessage = ge.getMessage();
		}
		Assert.assertEquals("SomeFactory found incorrect root element in the plugin configuration: should be 'someRootElemName'.", exceptionMessage);
	}
	
}
