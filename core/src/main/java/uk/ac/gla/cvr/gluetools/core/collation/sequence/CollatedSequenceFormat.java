package uk.ac.gla.cvr.gluetools.core.collation.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.SequenceFormatException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public enum CollatedSequenceFormat {

	GENBANK_FLAT_FILE {
		@Override
		public Document asXml(String sequenceText) {
			return GenbankFlatFileUtils.genbankFlatFileToXml(sequenceText);
		}
	},
	GENBANK_XML {
		@Override
		public Document asXml(String sequenceText) {
			try {
				return XmlUtils.documentFromStream(new ByteArrayInputStream(sequenceText.getBytes()));
			} catch (SAXException e) {
				throw new SequenceFormatException(e, Code.MALFORMED_XML, e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	public abstract Document asXml(String sequenceText);
}
