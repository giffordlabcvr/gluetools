package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import freemarker.template.Configuration;

public class TestNcbiGenbank {

	//@Test
	public void retrieveAllHcvIncludedAsXml() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testRetrieveAllHcvIncludedAsXml.xml"));
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration());
		NcbiImporterPlugin sequenceSourcer = (NcbiImporterPlugin) PluginFactory.get(ModulePluginFactory.creator).
					createFromElement(pluginConfigContext, document.getDocumentElement());
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
