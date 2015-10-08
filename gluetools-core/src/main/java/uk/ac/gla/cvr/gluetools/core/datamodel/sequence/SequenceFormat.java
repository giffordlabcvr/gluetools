package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.utils.ByteScanningUtils;

public enum SequenceFormat {

	// this MUST come before FASTA, so that it detection from bytes happens correctly.
	SAM2CONSENSUS_EXTENDED("SAM2CONSENSUS extended", null, "s2c") {
		@Override
		public boolean detectFromBytes(byte[] data) {
			return data.length > 0 &&
					data[0] == (byte) '>' &&
					ByteScanningUtils.indexOf(data, "Position,".getBytes(), 0) > 0;
		}
		@Override
		public AbstractSequenceObject sequenceObject() {
			return new Sam2ConsensusSequenceObject();
		}
	},

	FASTA("FASTA nucleic acid", "https://en.wikipedia.org/wiki/FASTA_format", "fasta") {
		@Override
		public boolean detectFromBytes(byte[] data) {
			return data.length > 0 &&
					data[0] == (byte) '>';
		}
		@Override
		public AbstractSequenceObject sequenceObject() {
			return new FastaSequenceObject();
		}
	},
	
	GENBANK_XML("Genbank GBSeq XML", "http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/asn_spec/gbseq.asn.html", "xml") {
		@Override
		public boolean detectFromBytes(byte[] data) {
			return ByteScanningUtils.indexOf(data, "<GBSeq>".getBytes(), 0) >= 0;
		}
		@Override
		public AbstractSequenceObject sequenceObject() {
			return new GenbankXmlSequenceObject();
		}
	}; 




	private String displayName;
	private String formatURL;
	private String standardFileExtension;
	
	private SequenceFormat(String displayName, String formatURL, String standardFileExtension) {
		this.displayName = displayName;
		this.formatURL = formatURL;
		this.standardFileExtension = standardFileExtension;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public String getFormatURL() {
		return formatURL;
	}
	public String getStandardFileExtension() {
		return standardFileExtension;
	}

	public abstract AbstractSequenceObject sequenceObject();
	
	public abstract boolean detectFromBytes(byte[] data);
	
	public static SequenceFormat detectFormatFromBytes(byte[] sequenceData) {
		for(SequenceFormat seqFormat : SequenceFormat.values()) {
			if(seqFormat.detectFromBytes(sequenceData)) {
				return seqFormat;
			}
		}
		throw new SequenceException(SequenceException.Code.UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_BYTES);
	}

	public static SequenceFormat detectFormatFromExtension(String extension) {
		for(SequenceFormat seqFormat : SequenceFormat.values()) {
			if(extension.equals(seqFormat.getStandardFileExtension())) {
				return seqFormat;
			}
		}
		throw new SequenceException(SequenceException.Code.UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_FILE_EXTENSION, extension);
	}

	
	
	
	
}
