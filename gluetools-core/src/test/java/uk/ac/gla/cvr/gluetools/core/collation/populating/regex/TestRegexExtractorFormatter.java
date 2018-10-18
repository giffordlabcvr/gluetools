/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.populating.regex;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import freemarker.template.Configuration;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


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
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration(Configuration.VERSION_2_3_24));
		RegexExtractorFormatter regexExtractorFormatter = 
				PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class,
				document.getDocumentElement());
		return regexExtractorFormatter;
	}
	
}
