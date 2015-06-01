package uk.ac.gla.cvr.gluetools.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.loader.GenbankProxySequenceReader;


public class GenbankFile {
	
	public static void main(String[] args) throws Exception {
		GenbankProxySequenceReader<NucleotideCompound> genbankDNAReader 
			= new GenbankProxySequenceReader<NucleotideCompound>("/tmp", "JX183551.1", AmbiguityDNACompoundSet.getDNACompoundSet());
		DNASequence dnaSequence = new DNASequence(genbankDNAReader);
		genbankDNAReader.getHeaderParser().parseHeader(genbankDNAReader.getHeader(), dnaSequence);
		System.out.println("Sequence" + "(" + dnaSequence.getAccession() + "," + dnaSequence.getLength() + ")=" +
				dnaSequence.getSequenceAsString().substring(0, 10) + "...");
		
		System.out.println("Header:"+genbankDNAReader.getHeader());
		System.out.println("Length:"+genbankDNAReader.getLength());
		System.out.println("Key words:"+genbankDNAReader.getKeyWords());
		@SuppressWarnings("rawtypes")
		HashMap<String, ArrayList<AbstractFeature>> features = genbankDNAReader.getFeatures();
		features.forEach((name, fList) -> {
			System.out.println("Key: "+name);
			fList.forEach(f -> {
				System.out.println("Location:"+f.getSource());
				HashMap<String, Qualifier> qualifiers = f.getQualifiers();
				System.out.println("Qualifiers:");
				qualifiers.forEach((qName, q) -> {
					System.out.println(q.getName() + " = " + q.getValue());
				});
			});
		});
	}
}
