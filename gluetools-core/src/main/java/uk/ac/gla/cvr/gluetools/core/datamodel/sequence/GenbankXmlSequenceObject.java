package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class GenbankXmlSequenceObject extends AbstractSequenceObject {

	public static long msInXPath;
	public static long msInDocParsing;
	
	private String header;
	private String nucleotides;
	private Document document;

	public GenbankXmlSequenceObject() {
		super(SequenceFormat.GENBANK_XML);
	}

	@Override
	protected String getNucleotidesInternal(CommandContext cmdContext) {
		if(nucleotides == null) {
			nucleotides = extractNucleotides(cmdContext);
		}
		return nucleotides;
	}

	private String extractNucleotides(CommandContext cmdContext) {
		long xpathStartTime = System.currentTimeMillis();
		XPathExpression xPathExpression = GlueXmlUtils.createXPathExpression(cmdContext.getXpathEngine(), "/GBSeq/GBSeq_sequence/text()");
		String seqString = GlueXmlUtils.getXPathString(document, xPathExpression);
		msInXPath = msInXPath + (System.currentTimeMillis() - xpathStartTime);
		if(seqString == null) {
			String primaryAccession = GlueXmlUtils.getXPathString(document, "/GBSeq/GBSeq_primary-accession/text()");
			if(primaryAccession == null || primaryAccession.length() == 0) {
				primaryAccession = "unknown";
			}
			throw new SequenceException(SequenceException.Code.XML_SEQUENCE_DOES_NOT_CONTAIN_NUCLEOTIDES, primaryAccession);
		}
		return seqString.replaceAll("\\s", "");
	}

	@Override
	public byte[] toOriginalData() {
		return GlueXmlUtils.prettyPrint(this.document);
	}

	@Override
	public void fromOriginalData(byte[] originalData) {
		long docParsingStartTime = System.currentTimeMillis();
		try {
			this.document = GlueXmlUtils.documentFromStream(new ByteArrayInputStream(originalData));
			msInDocParsing = msInDocParsing + (System.currentTimeMillis() - docParsingStartTime);
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
