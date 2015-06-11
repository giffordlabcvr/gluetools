package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankFeatureRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankQualifierRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankSimpleFieldRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.FieldPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.XPathNodesRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class PopulatorRuleFactory extends PluginFactory<PopulatorRule>{

	public static Multiton.Creator<PopulatorRuleFactory> creator = new
			Multiton.SuppliedCreator<>(PopulatorRuleFactory.class, PopulatorRuleFactory::new);
	
	private PopulatorRuleFactory() {
		super();
		registerPluginClass(FieldPopulatorRule.class);
		registerPluginClass(XPathNodesRule.class);
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
