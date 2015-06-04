package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequenceFormat;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileUtils;
import uk.ac.gla.cvr.gluetools.core.datafield.StringField;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulator;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


public class TestXmlPopulator {
	
	@Test 
	public void testXmlPopulator1() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testXmlPopulator1.xml"));
		DataFieldPopulator dataFieldPopulator = PluginFactory.get(DataFieldPopulatorFactory.creator).createFromElement(document.getDocumentElement());
		Project project = new Project();
		project.addDataField(new StringField("GENBANK_GENOTYPE"));
		String fileContent;
		try(InputStream inputStream = getClass().getResourceAsStream("testXmlPopulator1.gb")) {	
			fileContent = IOUtils.toString(inputStream);
		}
		List<String> gbStrings = GenbankFlatFileUtils.divideConcatenatedGBFiles(fileContent);
		List<CollatedSequence> collatedSequences = gbStrings.stream().map(string -> {
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setOwningProject(project);
			collatedSequence.setFormat(CollatedSequenceFormat.GENBANK_FLAT_FILE);
			collatedSequence.setSequenceText(string);
			return collatedSequence;
		}).collect(Collectors.toList());
		collatedSequences.forEach(sequence -> {
			dataFieldPopulator.populate(sequence);
			System.out.println(sequence.getDataFieldValue("GENBANK_GENOTYPE").getValue());
		});
		
	}
}
