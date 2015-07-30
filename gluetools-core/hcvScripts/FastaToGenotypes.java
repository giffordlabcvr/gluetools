

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

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FastaToGenotypes {

	private static class Sequence {
		String genotype = null;
		String subtype = null;
		String id;
		String data;
		public String toString() { return genotype+"-"+subtype+":"+id; }
	}
	
	public static void main(String[] args) throws Exception {

		// if false, unassigned subtypes e.g. 1_AJ851228 will be given their own taxa.
		// if true, they will be excluded from the cluster tree and from the fasta file.
		
		// TODO -- unassigned subtype sequences are grouped with possibly more than one per subtype.
		// see the paper.
		
		boolean excludeSequencesWithUnassignedSubtypes = true;
		
		String directory = "/Users/joshsinger/gitrepos/gluetools/gluetools-core/hcv";
		String filename = "Donald_Smith_HCV_alignment_May_26_2015.fasta";
		
		// The filtered file does not include reference sequences for unassigned subtypes.
		boolean fileFiltered = false;
	    
		// may need to update pattern to extract genotype string from FASTA seq id
		Pattern pattern = Pattern.compile("(\\d[a-z]*)\\??_.*");;
	    
	    List<Sequence> unfilteredSequences = new ArrayList<Sequence>();
	    
	    Map<String, DNASequence> fastaContent;
	    try(FileInputStream fis = new FileInputStream(new File(directory, filename))) {
	    	fastaContent = FastaUtils.parseFasta(IOUtils.toByteArray(fis));
	    }
	    
		fastaContent.forEach((fastaID, dnaSequence) -> {
        	Matcher matcher = pattern.matcher(fastaID);
        	if(matcher.find()) {
	        	Sequence sequence = new Sequence();
	        	sequence.id = fastaID;
				String genotypeSubtype = matcher.group(1);
				sequence.genotype = genotypeSubtype.substring(0, 1);
				sequence.subtype = genotypeSubtype.substring(1);
				if(sequence.subtype.trim().length() == 0) {
					sequence.subtype = "unassigned:"+sequence.id;
				}
				sequence.data = dnaSequence.getSequenceAsString();
	        	unfilteredSequences.add(sequence);
			}
			
		});
		
		
		List<Sequence> sequences = unfilteredSequences;
		if(excludeSequencesWithUnassignedSubtypes) {
			sequences = unfilteredSequences.stream().filter(seq -> !seq.subtype.startsWith("unassigned:")).collect(Collectors.toList());
			fileFiltered = true;
		}
		
		if(fileFiltered) {
			String newFilename = filename.replace(".fasta", ".filtered.fasta");
			try(PrintWriter writer = new PrintWriter(new File(directory, newFilename))) {
				sequences.forEach(seq -> {
					writer.println(">"+seq.id);
					writer.println(seq.data);
					writer.flush();
				});
			}
			filename = newFilename;
		}
		
		Map<String, List<Sequence>> genotypeToSequences = sequences.stream().collect(Collectors.groupingBy(seq -> seq.genotype));
		Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences = new LinkedHashMap<String, Map<String, List<Sequence>>>();
		genotypeToSequences.forEach( (genotype, genotypeSeqs) -> {
			genotypeToSubtypeToSequences.put(genotype, genotypeSeqs.stream().collect(Collectors.groupingBy(seq -> seq.subtype)));
		});
		
		JsonObject jsonGenotypeTree = generateJsonGenotypeTree(genotypeToSubtypeToSequences);
		try(PrintWriter pw = new PrintWriter(new File(directory, "hcvGenotypes.json"))) {
			pw.write(jsonGenotypeTree.toString());
		}


		Document clusterTreeDoc = generateRegaClusterTree(genotypeToSubtypeToSequences);
		try(FileOutputStream fos = new FileOutputStream(new File(directory, "hcv.xml"))) {
			XmlUtils.prettyPrint(clusterTreeDoc, fos, 4);
		}

		
		
		
	}




	
	private static JsonObject generateJsonGenotypeTree(Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences) {
		JsonObjectBuilder jsonTreeBuilder = JsonUtils.jsonObjectBuilder();
		jsonTreeBuilder.add("label", "All HCV");
		jsonTreeBuilder.add("id", "HCV");
		JsonArrayBuilder genotypesArrayBuilder = JsonUtils.jsonArrayBuilder();
		genotypeToSubtypeToSequences.forEach( (genotype, subtypeToSequences) -> {
			JsonObjectBuilder genotypeBuilder = JsonUtils.jsonObjectBuilder();
			String genotypeId = "genotype_"+genotype;
			genotypeBuilder.add("id", genotypeId);
			genotypeBuilder.add("label", "Genotype "+genotype);
			JsonArrayBuilder subtypesArrayBuilder = JsonUtils.jsonArrayBuilder();
			subtypeToSequences.forEach( (subtype, subtypeSeqs) -> {
				JsonObjectBuilder subtypeBuilder = JsonUtils.jsonObjectBuilder();
				String subtypeId;
				String subtypeLabel;
				if(subtype.startsWith("unassigned:")) {
					String unassignedSequence = subtype.replaceFirst("unassigned:.*_", "");
					subtypeId = genotype+"_unassigned_"+unassignedSequence;
					subtypeLabel = "Unassigned subtype ("+unassignedSequence+")";
				} else {
					subtypeId = "subtype_"+genotype+subtype;
					subtypeLabel = "Subtype "+genotype+subtype;
				}
				subtypeBuilder.add("id", subtypeId);
				subtypeBuilder.add("label", subtypeLabel);

				JsonArrayBuilder sequencesArrayBuilder = JsonUtils.jsonArrayBuilder();

				subtypeSeqs.forEach(seq -> {
					sequencesArrayBuilder.add(seq.id.replaceFirst(".*_", ""));
				});
				subtypeBuilder.add("sequences", sequencesArrayBuilder);
				subtypesArrayBuilder.add(subtypeBuilder);
			});
			genotypeBuilder.add("children", subtypesArrayBuilder);
			genotypesArrayBuilder.add(genotypeBuilder);
		});
		jsonTreeBuilder.add("children", genotypesArrayBuilder);
		return jsonTreeBuilder.build();
	}


	private static Document generateRegaClusterTree(Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences) {
		List<String> clusterIds = new ArrayList<String>();
		Element clustersElem = XmlUtils.documentWithElement("clusters");
		Document doc = clustersElem.getOwnerDocument();
		genotypeToSubtypeToSequences.forEach( (genotype, subtypeToSequences) -> {
			Element parentElem = clustersElem;
			if(subtypeToSequences.size() > 1) {
				Element genotypeClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
				String genotypeClusterId = "Geno_"+genotype;
				clusterIds.add(genotypeClusterId);
				genotypeClusterElem.setAttribute("id", genotypeClusterId);
				genotypeClusterElem.setAttribute("name", "Genotype "+genotype);
				parentElem = genotypeClusterElem;
			}
			final Element subtypeParentElem = parentElem;
			subtypeToSequences.forEach( (subtype, subtypeSeqs) -> {
				Element subtypeClusterElem = (Element) subtypeParentElem.appendChild(doc.createElement("cluster"));
				String subtypeClusterId;
				String subtypeDesc;
				if(subtype.startsWith("unassigned:")) {
					subtypeDesc = "Genotype "+genotype+" unassigned subtype defined by sequence "+subtype.replace("unassigned:", "");
					subtypeClusterId = genotype+":"+subtype;
				} else {
					subtypeDesc = "Genotype "+genotype+" subtype "+subtype;
					subtypeClusterId = genotype+subtype;
				}
				clusterIds.add(subtypeClusterId);
				subtypeClusterElem.setAttribute("id", subtypeClusterId);
				subtypeClusterElem.setAttribute("name", subtypeDesc);
				subtypeSeqs.forEach(seq -> {
					Element taxusElem = (Element) subtypeClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", seq.id);
				});
			});
		});
		
		return doc;
	}

	
}
