package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankFeatureRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankOrganismRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankPrimaryAccessionRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankQualifierRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank.GenbankSeqDivisionRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.FieldPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.XPathNodesRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class PopulatorRuleFactory extends PluginFactory<PopulatorRule>{

	public static Multiton.Creator<PopulatorRuleFactory> creator = new
			Multiton.SuppliedCreator<>(PopulatorRuleFactory.class, PopulatorRuleFactory::new);
	
	private PopulatorRuleFactory() {
		super();
		registerPluginClass(FieldPopulatorRule.ELEM_NAME, FieldPopulatorRule.class);
		registerPluginClass(XPathNodesRule.ELEM_NAME, XPathNodesRule.class);
		registerPluginClass(GenbankFeatureRule.ELEM_NAME, GenbankFeatureRule.class);
		registerPluginClass(GenbankQualifierRule.ELEM_NAME, GenbankQualifierRule.class);
		registerPluginClass(GenbankPrimaryAccessionRule.ELEM_NAME, GenbankPrimaryAccessionRule.class);
		registerPluginClass(GenbankOrganismRule.ELEM_NAME, GenbankOrganismRule.class);
		registerPluginClass(GenbankSeqDivisionRule.ELEM_NAME, GenbankSeqDivisionRule.class);
	}
	
}
