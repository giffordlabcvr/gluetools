package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.FastaSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;

public class FastaUtils {
	
	public static Map<String, DNASequence> parseFasta(byte[] fastaBytes,
			SequenceHeaderParserInterface<DNASequence, NucleotideCompound> headerParser) {
		ByteArrayInputStream bais = new ByteArrayInputStream(fastaBytes);
		FastaReader<DNASequence, NucleotideCompound> fastaReader = 
				new FastaReader<DNASequence, NucleotideCompound>(bais, headerParser, 
						new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()));
		Map<String, DNASequence> idToSequence;
		try {
			idToSequence = fastaReader.process();
		} catch (IOException e) {
			throw new SequenceException(e, Code.SEQUENCE_FORMAT_ERROR, "FASTA format error");
		}
		return idToSequence;
	}

	public static Map<String, DNASequence> parseFasta(byte[] fastaBytes) {
		return parseFasta(fastaBytes, new TrivialHeaderParser());
	}
	
	private static class TrivialHeaderParser implements SequenceHeaderParserInterface<DNASequence, NucleotideCompound> {
		@Override
		public void parseHeader(String header, DNASequence sequence) {
			sequence.setAccession(new AccessionID(header));
		}
	}

	public static byte[] mapToFasta(Map<String, DNASequence> sequenceIdToNucleotides) {
		final StringBuffer buf = new StringBuffer();
		sequenceIdToNucleotides.forEach((seqId, dnaSequence) -> 
			buf.append(seqIdNtsPairToFasta(seqId, dnaSequence.toString())));
		return buf.toString().getBytes();
	}
	
	
	public static String seqIdNtsPairToFasta(String seqId, String ntsString) {
		final StringBuffer buf = new StringBuffer();
		buf.append(">").append(seqId).append("\n");
		int start = 0;
		int blockLen = 70;
		while(start + blockLen < ntsString.length()) {
			buf.append(ntsString.substring(start, start+blockLen));
			buf.append("\n");
			start = start+blockLen;
		}
		if(start < ntsString.length()) {
			buf.append(ntsString.substring(start));
			buf.append("\n");
		}
		return buf.toString();
		
	}

	public static List<AbstractSequenceObject> seqObjectsFromSeqData(
			byte[] sequenceData) {
		SequenceFormat format = SequenceFormat.detectFormatFromBytes(sequenceData);
		List<AbstractSequenceObject> seqObjects;
		if(format == SequenceFormat.FASTA) {
			Map<String, DNASequence> fastaMap = parseFasta(sequenceData);
			seqObjects = fastaMap.entrySet().stream()
					.map(ent -> new FastaSequenceObject(ent.getKey(), ent.getValue().toString()))
					.collect(Collectors.toList());
		} else {
			AbstractSequenceObject seqObj = format.sequenceObject();
			seqObj.fromOriginalData(sequenceData);
			seqObjects = Collections.singletonList(seqObj);
		}
		return seqObjects;
	}

	public static char nt(String nucleotides, int position) {
		return nucleotides.charAt(position-1);
	}

	public static int find(String nucleotides, String subSequence, int from) {
		int index = nucleotides.indexOf(subSequence, from-1);
		if(index == -1) { return index; }
		return index+1;
	}

	public static CharSequence subSequence(String nucleotides, int start, int end) {
		return nucleotides.subSequence(start-1, end);
	}
	
}
