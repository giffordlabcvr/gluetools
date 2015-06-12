package uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class GenbankSimpleFieldRule extends NodeSelectorRule {

	private String xPathString;
	
	@Override
	public final void configureLocal(PluginConfigContext pluginConfigContext, Element configElem) {
		try {
			setXPathExpression(XmlUtils.createXPathEngine().compile(xPathString));
		} catch (XPathExpressionException xpee) {
			throw new RuntimeException(xpee);
		}
	}

	public GenbankSimpleFieldRule(String xPathString) {
		super();
		this.xPathString = xPathString;
	}
	
	public static class Organism extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbOrganism";
		public Organism() { super("/GBSeq/GBSeq_organism/text()"); }	
	}

	public static class PrimaryAccession extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbPrimaryAccession";
		public PrimaryAccession() { super("/GBSeq/GBSeq_primary-accession/text()"); }	
	}

	public static class AccessionVersion extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbAccessionVersion";
		public AccessionVersion() { super("/GBSeq/GBSeq_accession-version/text()"); }	
	}
	
	public static class Division extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbDivision";
		public Division() { super("/GBSeq/GBSeq_division/text()"); }	
	}

	public static class Taxonomy extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbTaxonomy";
		public Taxonomy() { super("/GBSeq/GBSeq_taxonomy/text()"); }	
	}

	public static class Locus extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbLocus";
		public Locus() { super("/GBSeq/GBSeq_locus/text()"); }	
	}
	
	public static class Length extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbLength";
		public Length() { super("/GBSeq/GBSeq_length/text()"); }	
	}

	public static class OtherID extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbOtherID";
		public OtherID() { super("/GBSeq/GBSeq_other-seqids/GBSeqid/text()"); }	
	}

	public static class UpdateDate extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbUpdateDate";
		public UpdateDate() { super("/GBSeq/GBSeq_update-date/text()"); }	
	}

	public static class CreateDate extends GenbankSimpleFieldRule {
		public static final String ELEM_NAME = "gbCreateDate";
		public CreateDate() { super("/GBSeq/GBSeq_create-date/text()"); }	
	}

}
