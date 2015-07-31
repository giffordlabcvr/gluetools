import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class FastaToNcbiQuery {

	public static void main(String[] args) throws Exception {

		String directory = "/Users/joshsinger/gitrepos/gluetools/gluetools-core/hcvProject";
		String filename = "Donald_Smith_HCV_alignment_May_26_2015.filtered.fasta";
		
	    Map<String, DNASequence> fastaContent;
	    try(FileInputStream fis = new FileInputStream(new File(directory, filename))) {
	    	fastaContent = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }
	    
	    List<String> primaryAccs = new ArrayList<String>();
		fastaContent.forEach((fastaID, dnaSequence) -> {
			String primaryAcc = fastaID.replaceAll(".*_", "");
			primaryAccs.add(primaryAcc);
		});	
		String eSearchTerm = String.join(" OR ",
				primaryAccs.stream().map(s -> "\""+s+"\"[Primary Accession]").collect(Collectors.toList()));
		
		
		Element ncbiImporterElem = GlueXmlUtils.documentWithElement("ncbiImporter");
		GlueXmlUtils.appendElementWithText(ncbiImporterElem, "sequenceFormat", SequenceFormat.GENBANK_XML.name());
		GlueXmlUtils.appendElementWithText(ncbiImporterElem, "eSearchTerm", eSearchTerm);
		GlueXmlUtils.appendElementWithText(ncbiImporterElem, "eFetchBatchSize", "500");
		
		GlueXmlUtils.prettyPrint(ncbiImporterElem.getOwnerDocument(), System.out);
		
	}
}
