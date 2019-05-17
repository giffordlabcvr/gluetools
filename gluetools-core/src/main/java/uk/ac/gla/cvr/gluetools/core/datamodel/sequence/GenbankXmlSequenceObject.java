/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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

	public GenbankXmlSequenceObject(Sequence sequence) {
		super(SequenceFormat.GENBANK_XML, sequence);
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
		return seqString.replaceAll("\\s", "").toUpperCase();
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
