package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporter.RetrievedSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import freemarker.template.Configuration;

public class TestNcbiGenbank {

	//@Test
	public void retrieveAllHcvIncludedAsXml() throws Exception {
		Document document = GlueXmlUtils.documentFromStream(getClass().getResourceAsStream("testRetrieveAllHcvIncludedAsXml.xml"));
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration());
		NcbiImporter ncbiImporter = (NcbiImporter) PluginFactory.get(ModulePluginFactory.creator).
					createFromElement(pluginConfigContext, document.getDocumentElement());
		List<String> sequenceIDs = ncbiImporter.getGiNumbersToRetrieve();
		List<RetrievedSequence> retrievedSequences = ncbiImporter.retrieveSequences(sequenceIDs);
		File directory = new File("/Users/joshsinger/hcv_rega/retrieved_xml");
		retrievedSequences.forEach(seq -> {
			File xmlFile = new File(directory, seq.giNumber+".xml");
			try(FileOutputStream fileOutputStream = new FileOutputStream(xmlFile)) {
				fileOutputStream.write(seq.data);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
	
}
