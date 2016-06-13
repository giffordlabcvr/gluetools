package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.utils.ByteScanningUtils;

public enum SequenceFormat {


	FASTA("FASTA nucleic acid", "https://en.wikipedia.org/wiki/FASTA_format", 
			FastaSequenceObject.FASTA_ACCEPTED_EXTENSIONS, 
			FastaSequenceObject.FASTA_DEFAULT_EXTENSION) {
		@Override
		public boolean detectFromBytes(byte[] data) {
			return data.length > 0 &&
					data[0] == (byte) '>';
		}
		@Override
		public AbstractSequenceObject sequenceObject() {
			return new FastaSequenceObject();
		}
		@Override
		public String getGeneratedFileExtension(CommandContext cmdContext) {
			return cmdContext.getProjectSettingValue(ProjectSettingOption.EXPORTED_FASTA_EXTENSION);
		}
		
		
	},
	
	GENBANK_XML("Genbank GBSeq XML", "http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/asn_spec/gbseq.asn.html", new String[]{"xml"}, "xml") {
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
	private String[] acceptedFileExtensions;
	private String generatedFileExtension;
	
	private SequenceFormat(String displayName, String formatURL, String[] acceptedFileExtensions, String generatedFileExtension) {
		this.displayName = displayName;
		this.formatURL = formatURL;
		this.acceptedFileExtensions = acceptedFileExtensions;
		this.generatedFileExtension = generatedFileExtension;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public String getFormatURL() {
		return formatURL;
	}
	public String[] getAcceptedFileExtensions() {
		return acceptedFileExtensions;
	}
	public String getGeneratedFileExtension(CommandContext cmdContext) {
		return generatedFileExtension;
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
		return detectFormatFromExtension(extension, false);
	}
	
	public static SequenceFormat detectFormatFromExtension(String extension, boolean allowNull) {
		for(SequenceFormat seqFormat : SequenceFormat.values()) {
			if(Arrays.asList(seqFormat.getAcceptedFileExtensions()).contains(extension)) {
				return seqFormat;
			}
		}
		if(allowNull) {
			return null;
		}
		throw new SequenceException(SequenceException.Code.UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_FILE_EXTENSION, extension);
	}

	
	
	
	
}
