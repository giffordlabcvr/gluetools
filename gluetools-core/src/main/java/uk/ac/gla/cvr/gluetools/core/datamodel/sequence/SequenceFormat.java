package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
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
		public CommandResult showDataResult(byte[] data) {
			try {
				return new DocumentResult(XmlUtils.documentFromStream(new ByteArrayInputStream(data)));
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
			FastaUtils.parseFasta(data);
		}

		@Override
		public CommandResult showDataResult(byte[] data) {
			return new SimpleConsoleCommandResult(new String(data));
		}
	};

	public abstract void validateFormat(byte[] data);

	public abstract CommandResult showDataResult(byte[] data);
}
