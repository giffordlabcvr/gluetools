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
package uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.GenbankXmlSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="gbRefBuilder",
		includeInWebDocs=false)
public class GbRefBuilder extends ModulePlugin<GbRefBuilder> {

	private List<GbFeatureLocationRule> gbFeatureLocationRules;
	
	public GbRefBuilder() {
		super();
		registerModulePluginCmdClass(BuildReferenceCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> gbFeatureLocationRuleElems = PluginUtils.findConfigElements(configElem, "gbFeatureLocationRule");
		this.gbFeatureLocationRules = 
				PluginFactory.createPlugins(pluginConfigContext, GbFeatureLocationRule.class, gbFeatureLocationRuleElems);
	}
	
	public CreateResult buildReference(CommandContext cmdContext, String refName, String sourceName, String sequenceID) {
		Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
		if(!sequence.getSequenceFormat().equals(SequenceFormat.GENBANK_XML)) {
			throw new GbRefBuilderException(GbRefBuilderException.Code.NOT_GENBANK_XML_FORMAT, sourceName, sequenceID); 
		}
		GenbankXmlSequenceObject gbXmlSeqObj = (GenbankXmlSequenceObject) sequence.getSequenceObject();
		Document gbXmlDoc = gbXmlSeqObj.getDocument();
		log("Creating reference "+refName+
				" from GB XML (sourceName:"+sourceName+
				", sequenceID:"+sequenceID+")");
		CreateResult createResult = cmdContext.cmdBuilder(CreateReferenceSequenceCommand.class)
				.set(CreateReferenceSequenceCommand.REF_SEQ_NAME, refName)
				.set(CreateReferenceSequenceCommand.SEQUENCE_ID, sequenceID)
				.set(CreateReferenceSequenceCommand.SOURCE_NAME, sourceName)
				.execute();

		List<Element> featureElements = GlueXmlUtils.getXPathElements(gbXmlDoc, "GBSeq/GBSeq_feature-table/GBFeature");
		Set<String> createdFeatureLocations = new LinkedHashSet<String>();
		try(ModeCloser referenceMode = cmdContext.pushCommandMode("reference", refName)) {
			for(Element featureElem: featureElements) {
				for(GbFeatureLocationRule gbFeatureLocationRule: gbFeatureLocationRules) {
					String ruleFeatureName = gbFeatureLocationRule.getFeatureName();
					if(!createdFeatureLocations.contains(ruleFeatureName)) {
						boolean featureCreated = gbFeatureLocationRule.run(cmdContext, featureElem, this);
						if(featureCreated) {
							createdFeatureLocations.add(ruleFeatureName);
						}
					}
				}
			}
		}
		return createResult;
	}

	
	
}
