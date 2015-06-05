package uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class GenbankPrimaryAccessionRule extends NodeSelectorRule {

	public static String ELEM_NAME = "gbPrimaryAccession";
	
	@Override
	public void configureLocal(Element configElem) {
		String xPathString = "/GBSeq/GBSeq_primary-accession/text()";
		try {
			setXPathExpression(XmlUtils.createXPathEngine().compile(xPathString));
		} catch (XPathExpressionException xpee) {
			throw new RuntimeException(xpee);
		}
	}

}
