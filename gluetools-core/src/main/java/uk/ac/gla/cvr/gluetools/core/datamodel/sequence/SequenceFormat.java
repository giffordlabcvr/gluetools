package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public enum SequenceFormat {


	GENBANK_XML {
		@Override
		public Document asXml(byte[] data) {
			try {
				return XmlUtils.documentFromStream(new ByteArrayInputStream(data));
			} catch (SAXException e) {
				throw new SequenceException(e, Code.MALFORMED_XML, e.getMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	public abstract Document asXml(byte[] data);
}
