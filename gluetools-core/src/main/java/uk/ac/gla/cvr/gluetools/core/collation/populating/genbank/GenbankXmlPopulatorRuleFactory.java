package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class GenbankXmlPopulatorRuleFactory extends XmlPopulatorRuleFactory {

	public static Multiton.Creator<GenbankXmlPopulatorRuleFactory> creator = new
			Multiton.SuppliedCreator<>(GenbankXmlPopulatorRuleFactory.class, GenbankXmlPopulatorRuleFactory::new);
	
	private GenbankXmlPopulatorRuleFactory() {
		super();
		registerPluginClass(GenbankFeatureRule.class);
		registerPluginClass(GenbankQualifierRule.class);
		registerPluginClass(GenbankSimpleFieldRule.Length.class);
		registerPluginClass(GenbankSimpleFieldRule.Locus.class);
		registerPluginClass(GenbankSimpleFieldRule.Organism.class);
		registerPluginClass(GenbankSimpleFieldRule.PrimaryAccession.class);
		registerPluginClass(GenbankSimpleFieldRule.AccessionVersion.class);
		registerPluginClass(GenbankSimpleFieldRule.Division.class);
		registerPluginClass(GenbankSimpleFieldRule.Taxonomy.class);
		registerPluginClass(GenbankSimpleFieldRule.OtherID.class);
		registerPluginClass(GenbankSimpleFieldRule.CreateDate.class);
		registerPluginClass(GenbankSimpleFieldRule.UpdateDate.class);
	}
	
}
