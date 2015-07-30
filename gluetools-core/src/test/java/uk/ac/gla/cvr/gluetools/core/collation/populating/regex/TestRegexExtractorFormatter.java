package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import freemarker.template.Configuration;


public class TestRegexExtractorFormatter {

	@Test
	public void test1() throws Exception {
		String testFile = "regexExtractorFormatter1.xml";
		RegexExtractorFormatter regexExtractorFormatter = loadTestFile(testFile);
		Assert.assertEquals("mook", regexExtractorFormatter.matchAndConvert("hello foo goodbye"));
	}

	@Test
	public void test2() throws Exception {
		String testFile = "regexExtractorFormatter2.xml";
		RegexExtractorFormatter regexExtractorFormatter = loadTestFile(testFile);
		Assert.assertEquals("thing: goodbye", regexExtractorFormatter.matchAndConvert("hello foo goodbye"));
	}

	@Test
	public void test3() throws Exception {
		String testFile = "regexExtractorFormatter3.xml";
		RegexExtractorFormatter regexExtractorFormatter = loadTestFile(testFile);
		Assert.assertEquals("hello foo", regexExtractorFormatter.matchAndConvert("hello foo goodbye"));
	}

	@Test
	public void test4() throws Exception {
		String testFile = "regexExtractorFormatter4.xml";
		RegexExtractorFormatter regexExtractorFormatter = loadTestFile(testFile);
		Assert.assertEquals("The hello thing: foo", regexExtractorFormatter.matchAndConvert("hello foo goodbye"));
	}

	@Test
	public void test5() throws Exception {
		String testFile = "regexExtractorFormatter5.xml";
		RegexExtractorFormatter regexExtractorFormatter = loadTestFile(testFile);
		Assert.assertEquals("thing: goodbye", regexExtractorFormatter.matchAndConvert("hello foo GOODBYE"));
	}

	
	private RegexExtractorFormatter loadTestFile(String testFile)
			throws SAXException, IOException {
		Document document = GlueXmlUtils.documentFromStream(getClass().getResourceAsStream(testFile));
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration());
		RegexExtractorFormatter regexExtractorFormatter = 
				PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class,
				document.getDocumentElement());
		return regexExtractorFormatter;
	}
	
}
