

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class FastaToGlueGenotypeCommands {

	
	public static void main(String[] args) throws Exception {

		String directory = "/Users/joshsinger/gitrepos/gluetools/gluetools-core/hcvProject";
		String filename = "Donald_Smith_HCV_alignment_May_26_2015.filtered.fasta";
		
		// may need to update pattern to extract genotype string from FASTA seq id
		Pattern pattern = Pattern.compile("(\\d[a-z]*)\\??_.*");
	    
	    Map<String, DNASequence> fastaContent;
	    try(FileInputStream fis = new FileInputStream(new File(directory, filename))) {
	    	fastaContent = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }
	    
	    fastaContent.forEach((fastaID, dnaSequence) -> {
	    	Matcher matcher = pattern.matcher(fastaID);
	    	matcher.find();
	    	String primaryAcc = fastaID.replaceAll(".*_", "");
	    	String genotypeSubtype = matcher.group(1);
	    	String genotype = genotypeSubtype.substring(0, 1);
	    	String subtype = genotypeSubtype.substring(1);
	    	if(subtype.trim().length() == 0) {
	    		subtype = "unassigned:"+fastaID;
	    	}
	    	System.out.println("  sequence -w \"GB_PRIMARY_ACCESSION = '"+primaryAcc+"'\"");
	    	System.out.println("    set field GENOTYPE "+genotype);
	    	System.out.println("    set field SUBTYPE "+subtype);
	    	System.out.println("    exit");
	    });


	}

}
