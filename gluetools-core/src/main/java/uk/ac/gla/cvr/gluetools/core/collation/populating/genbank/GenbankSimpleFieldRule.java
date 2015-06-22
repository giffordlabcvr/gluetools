package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
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
	
	@PluginClass(elemName="gbOrganism")
	public static class Organism extends GenbankSimpleFieldRule {
		public Organism() { super("/GBSeq/GBSeq_organism/text()"); }	
	}

	@PluginClass(elemName="gbPrimaryAccession")
	public static class PrimaryAccession extends GenbankSimpleFieldRule {
		public PrimaryAccession() { super("/GBSeq/GBSeq_primary-accession/text()"); }	
	}

	@PluginClass(elemName="gbAccessionVersion")
	public static class AccessionVersion extends GenbankSimpleFieldRule {
		public AccessionVersion() { super("/GBSeq/GBSeq_accession-version/text()"); }	
	}
	
	@PluginClass(elemName="gbDivision")
	public static class Division extends GenbankSimpleFieldRule {
		public Division() { super("/GBSeq/GBSeq_division/text()"); }	
	}

	@PluginClass(elemName="gbTaxonomy")
	public static class Taxonomy extends GenbankSimpleFieldRule {
		public Taxonomy() { super("/GBSeq/GBSeq_taxonomy/text()"); }	
	}

	@PluginClass(elemName="gbLocus")
	public static class Locus extends GenbankSimpleFieldRule {
		public Locus() { super("/GBSeq/GBSeq_locus/text()"); }	
	}
	
	@PluginClass(elemName="gbLength")
	public static class Length extends GenbankSimpleFieldRule {
		public Length() { super("/GBSeq/GBSeq_length/text()"); }	
	}

	@PluginClass(elemName="gbOtherID")
	public static class OtherID extends GenbankSimpleFieldRule {
		public OtherID() { super("/GBSeq/GBSeq_other-seqids/GBSeqid/text()"); }	
	}

	@PluginClass(elemName="gbUpdateDate")
	public static class UpdateDate extends GenbankSimpleFieldRule {
		public UpdateDate() { super("/GBSeq/GBSeq_update-date/text()"); }	
	}

	@PluginClass(elemName="gbCreateDate")
	public static class CreateDate extends GenbankSimpleFieldRule {
		public CreateDate() { super("/GBSeq/GBSeq_create-date/text()"); }	
	}

}
