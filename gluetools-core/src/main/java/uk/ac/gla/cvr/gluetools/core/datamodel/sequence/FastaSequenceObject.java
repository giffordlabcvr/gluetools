package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.Map;
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class FastaSequenceObject extends AbstractSequenceObject {

	public static final String FASTA_DEFAULT_EXTENSION = "fasta";
	public static final String[] FASTA_ACCEPTED_EXTENSIONS = new String[]{"fasta", "fa", "fna", "fas"};

	
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
	protected String getNucleotidesInternal(CommandContext cmdContext) {
		return nucleotides;
	}

	@Override
	public byte[] toOriginalData() {
		return FastaUtils.seqIdCompoundsPairToFasta(getHeader(), nucleotides, LineFeedStyle.LF).getBytes();
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
