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
		registerPluginClass(GenbankReferenceRule.class);
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
		registerPluginClass(GenbankSimpleFieldRule.ReferenceTitle.class);
		registerPluginClass(GenbankSimpleFieldRule.ReferencePubmed.class);
		registerPluginClass(GenbankSimpleFieldRule.RefDoi.class);
		registerPluginClass(GenbankSimpleFieldRule.RefJournal.class);
		registerPluginClass(GenbankSimpleFieldRule.RefPubmed.class);
		registerPluginClass(GenbankSimpleFieldRule.RefTitle.class);
	}
	
}
