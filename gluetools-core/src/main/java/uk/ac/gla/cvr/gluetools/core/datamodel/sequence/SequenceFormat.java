package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaUtils;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public enum SequenceFormat {


	GENBANK_XML {
		@Override
		public void validateFormat(byte[] data) {
			try {
				XmlUtils.documentFromStream(new ByteArrayInputStream(data));
			} catch (SAXException e) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String originalDataAsString(byte[] data) {
			try {
				return new String(XmlUtils.prettyPrint(XmlUtils.documentFromStream(new ByteArrayInputStream(data))));
			} catch (SAXException e) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}, 
	
	FASTA {
		@Override
		public void validateFormat(byte[] data) {
			Map<String, DNASequence> fastaMap = FastaUtils.parseFasta(data);
			if(fastaMap.size() == 0) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Zero sequences found in FASTA string");
			}
			if(fastaMap.size() > 1) {
				throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, "Multiple sequences found in FASTA string");
			}
		}

		@Override
		public String originalDataAsString(byte[] data) {
			return new String(data);
		}
	};

	public abstract void validateFormat(byte[] data);

	public abstract String originalDataAsString(byte[] data);
}
