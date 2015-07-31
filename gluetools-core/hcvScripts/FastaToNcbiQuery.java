import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import FastaToGenotypes.Sequence;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FastaToNcbiQuery {

	public static void main(String[] args) throws Exception {

		String directory = "/Users/joshsinger/gitrepos/gluetools/gluetools-core/hcvProject";
		String filename = "Donald_Smith_HCV_alignment_May_26_2015.filtered.fasta";
		
	    List<Sequence> unfilteredSequences = new ArrayList<Sequence>();
	    
	    Map<String, DNASequence> fastaContent;
	    try(FileInputStream fis = new FileInputStream(new File(directory, filename))) {
	    	fastaContent = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }
	    
		fastaContent.forEach((fastaID, dnaSequence) -> {
			System.out.println(fastaID);
		});	
	}
}
