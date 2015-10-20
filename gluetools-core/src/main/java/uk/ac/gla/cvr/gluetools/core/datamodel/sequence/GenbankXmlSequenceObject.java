package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class GenbankXmlSequenceObject extends AbstractSequenceObject {

	private String header;
	private String nucleotides;
	private Document document;

	public GenbankXmlSequenceObject() {
		super(SequenceFormat.GENBANK_XML);
	}

	@Override
	protected String getNucleotides() {
		if(nucleotides == null) {
			nucleotides = extractNucleotides();
		}
		return nucleotides;
	}

	private String extractNucleotides() {
		return GlueXmlUtils.getXPathString(document, "/GBSeq/GBSeq_sequence/text()").replaceAll("\\s", "").toUpperCase();
	}

	@Override
	public byte[] toOriginalData() {
		return GlueXmlUtils.prettyPrint(this.document);
	}

	@Override
	public void fromOriginalData(byte[] originalData) {
		try {
			this.document = GlueXmlUtils.documentFromStream(new ByteArrayInputStream(originalData));
		} catch (SAXException e) {
			throw new SequenceException(Code.SEQUENCE_FORMAT_ERROR, e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getHeader() {
		if(header == null) {
			header = extractHeader();
		}
		return header;
	}

	private String extractHeader() {
		return GlueXmlUtils.getXPathString(document, "/GBSeq/GBSeq_primary-accession/text()");
	}
	
	public Document getDocument() {
		return document;
	}

}
