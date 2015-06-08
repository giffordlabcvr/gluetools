package uk.ac.gla.cvr.gluetools.core.collation.genbank.ncbi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class TestNcbiGenbank {

	@Test
	public void testNcbiSourcerCreate() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testNcbiSourcerCreate.xml"));
		SequenceSourcer sequenceSourcer = PluginFactory.get(SequenceSourcerFactory.creator).createFromElement(document.getDocumentElement());
		Assert.assertEquals("NCBISequenceSourcer:nuccore:GENBANK_FLAT_FILE", sequenceSourcer.getSourceUniqueID());
	}

	@Test
	public void testNcbiSourcerRunLive1() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testNcbiSourcerRunLive1.xml"));
		SequenceSourcer sequenceSourcer = PluginFactory.get(SequenceSourcerFactory.creator).createFromElement(document.getDocumentElement());
		List<String> sequenceIDs = sequenceSourcer.getSequenceIDs().stream().sorted().collect(Collectors.toList());
		Assert.assertEquals(5, sequenceIDs.size());
		List<CollatedSequence> collatedSequences = sequenceSourcer.retrieveSequences(sequenceIDs);
		Assert.assertEquals(5, collatedSequences.size());
		collatedSequences.forEach(cs -> {
			Assert.assertEquals(sequenceSourcer.getSourceUniqueID(), cs.getSourceUniqueID());
		});
		List<String> sourceIDs = collatedSequences.stream().map(cs -> cs.getSequenceSourceID()).sorted().collect(Collectors.toList());
		Assert.assertEquals(sourceIDs, sequenceIDs);
	}

	@Test
	public void retrieveAllHcvIncludedAsXml() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testRetrieveAllHcvIncludedAsXml.xml"));
		SequenceSourcer sequenceSourcer = PluginFactory.get(SequenceSourcerFactory.creator).createFromElement(document.getDocumentElement());
		List<String> sequenceIDs = sequenceSourcer.getSequenceIDs();
		List<CollatedSequence> collatedSequences = sequenceSourcer.retrieveSequences(sequenceIDs);
		File directory = new File("/Users/joshsinger/hcv_rega/retrieved_xml");
		collatedSequences.forEach(seq -> {
			File xmlFile = new File(directory, seq.getSequenceSourceID()+".xml");
			try(FileOutputStream fileOutputStream = new FileOutputStream(xmlFile)) {
				XmlUtils.prettyPrint(seq.asXml(), fileOutputStream);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
	
}
