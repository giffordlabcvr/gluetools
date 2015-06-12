package uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class TestGBFlatFileUtils {

	@Test
	public void testDivideConcatenatedGBFiles() throws Exception  {
		String fileContent;
		try(InputStream inputStream = getClass().getResourceAsStream("testinput1.gb")) {	
			fileContent = IOUtils.toString(inputStream);
		}
		List<Object> gbStrings = GenbankFlatFileUtils.divideConcatenatedGBFiles(fileContent);
		Assert.assertEquals(4, gbStrings.size());
	}
	
	@Test
	public void testFeatureQualifiers() throws Exception  {
		String fileContent;
		try(InputStream inputStream = getClass().getResourceAsStream("testinput2.gb")) {	
			fileContent = IOUtils.toString(inputStream);
		}
		Document doc = GenbankFlatFileUtils.genbankFlatFileToXml(fileContent);
		XmlUtils.prettyPrint(doc, System.out);
		
		Assert.assertEquals("core protein", XmlUtils.getXPathString(doc, 
				"/GBSeq/GBSeq_feature-table/GBFeature[3]/GBFeature_quals/GBQualifier[4]/GBQualifier_value/text()"));
		
	}
	
	
	
}
