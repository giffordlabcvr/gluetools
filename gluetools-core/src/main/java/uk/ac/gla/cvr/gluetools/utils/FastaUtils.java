package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;

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
}