package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils.XmlNamespaceContext;

public class BlastResultBuilder {

	public static class BlastXPath {
		private XPath xPathEngine = initXPathEngine();
		private XPathExpression search = xpathExp(xPathEngine, "/b:BlastOutput2/b:report/b:Report/b:results/b:Results/b:search/b:Search");
		private XPathExpression queryTitleText = xpathExp(xPathEngine, "b:query-title/text()");
		private XPathExpression hitHits = xpathExp(xPathEngine, "b:hits/b:Hit");
		private XPathExpression descriptionTitleText = xpathExp(xPathEngine, "b:description/b:title/text()");
		private XPathExpression hspsHsp = xpathExp(xPathEngine, "b:hsps/b:Hsp");

		private XPathExpression bitScoreText = xpathExp(xPathEngine, "b:bit-score/text()");
		private XPathExpression scoreText = xpathExp(xPathEngine, "b:score/text()");
		private XPathExpression evalueText = xpathExp(xPathEngine, "b:evalue/text()");
		private XPathExpression identityText = xpathExp(xPathEngine, "b:identity/text()");
		private XPathExpression queryFromText = xpathExp(xPathEngine, "b:query-from/text()");
		private XPathExpression queryToText = xpathExp(xPathEngine, "b:query-to/text()");
		private XPathExpression hitFromText = xpathExp(xPathEngine, "b:hit-from/text()");
		private XPathExpression hitToText = xpathExp(xPathEngine, "b:hit-to/text()");
		private XPathExpression alignLenText = xpathExp(xPathEngine, "b:align-len/text()");
		private XPathExpression gapsText = xpathExp(xPathEngine, "b:gaps/text()");
		private XPathExpression qseqText = xpathExp(xPathEngine, "b:qseq/text()");
		private XPathExpression hseqText = xpathExp(xPathEngine, "b:hseq/text()");

	}
	
	public static BlastResult blastResultFromDocument(BlastXPath xPath, Document blastDoc) {
		BlastResult blastResult = new BlastResult();
		Element searchElem = GlueXmlUtils.getXPathElement(blastDoc, xPath.search);
		blastResult.setQueryFastaId(GlueXmlUtils.getXPathString(searchElem, xPath.queryTitleText));
		List<Element> hitElems = GlueXmlUtils.getXPathElements(searchElem, xPath.hitHits);
		for(Element hitElem: hitElems) {
			BlastHit blastHit = new BlastHit(blastResult);
			blastResult.addHit(blastHit);
			blastHit.setReferenceName(GlueXmlUtils.getXPathString(hitElem, xPath.descriptionTitleText));
			List<Element> hspElems = GlueXmlUtils.getXPathElements(hitElem, xPath.hspsHsp);
			for(Element hspElem : hspElems) {
				BlastHsp blastHsp = new BlastHsp(blastHit);
				blastHit.addHsp(blastHsp);
				blastHsp.setBitScore(GlueXmlUtils.getXPathDouble(hspElem, xPath.bitScoreText));
				blastHsp.setScore(GlueXmlUtils.getXPathInt(hspElem, xPath.scoreText));
				blastHsp.setEvalue(GlueXmlUtils.getXPathDouble(hspElem, xPath.evalueText));
				blastHsp.setIdentity(GlueXmlUtils.getXPathInt(hspElem, xPath.identityText));
				blastHsp.setQueryFrom(GlueXmlUtils.getXPathInt(hspElem, xPath.queryFromText));
				blastHsp.setQueryTo(GlueXmlUtils.getXPathInt(hspElem, xPath.queryToText));
				blastHsp.setHitFrom(GlueXmlUtils.getXPathInt(hspElem, xPath.hitFromText));
				blastHsp.setHitTo(GlueXmlUtils.getXPathInt(hspElem, xPath.hitToText));
				blastHsp.setAlignLen(GlueXmlUtils.getXPathInt(hspElem, xPath.alignLenText));
				blastHsp.setGaps(GlueXmlUtils.getXPathInt(hspElem, xPath.gapsText));
				blastHsp.setQseq(GlueXmlUtils.getXPathString(hspElem, xPath.qseqText));
				blastHsp.setHseq(GlueXmlUtils.getXPathString(hspElem, xPath.hseqText));
			}
		}
		return blastResult;
	}



	
	private static XPathExpression xpathExp(XPath xPathEngine, String xPathString) {
		XPathExpression searchResultExpression;
		try {
			searchResultExpression = xPathEngine.compile(xPathString);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return searchResultExpression;
	}

	private static XPath initXPathEngine() {
		XmlNamespaceContext namespaceContext = new XmlNamespaceContext();
		namespaceContext.addNamespace("b", "http://www.ncbi.nlm.nih.gov");
		XPath xPathEngine = GlueXmlUtils.createXPathEngine();
		xPathEngine.setNamespaceContext(namespaceContext);
		return xPathEngine;
	}
	
}
