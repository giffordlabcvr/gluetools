package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.Map;
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class FastaSequenceObject extends AbstractSequenceObject {

	private String header;
	private String nucleotides;
	
	public FastaSequenceObject() {
		super(SequenceFormat.FASTA);
	}
	
	public FastaSequenceObject(String header, String nucleotides) {
		this();
		this.header = header;
		this.nucleotides = nucleotides;
	}
	
	@Override
	public String getHeader() {
		return header;
	}

	@Override
	protected String getNucleotides() {
		return nucleotides;
	}

	@Override
	public byte[] toOriginalData() {
		return FastaUtils.seqIdNtsPairToFasta(getHeader(), nucleotides).getBytes();
	}

	@Override
	public void fromOriginalData(byte[] originalData) {
		Map<String, DNASequence> seqIdToDna = FastaUtils.parseFasta(originalData);
		if(seqIdToDna.size() == 0) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Zero sequences found in FASTA string");
		}
		if(seqIdToDna.size() > 1) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Multiple sequences found in FASTA string");
		}
		Entry<String, DNASequence> singleEntry = seqIdToDna.entrySet().iterator().next();
		this.header = singleEntry.getKey();
		this.nucleotides = singleEntry.getValue().toString();
	}

}
