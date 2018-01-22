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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "feature", "tree"},
		docoptUsages={"[<featureName>]"},
		docoptOptions={},
		description="Show a tree of features for which the reference has a location",
		furtherHelp="If <featureName> is supplied, the result tree will only contain locations of features which "+
		"are ancestors or descendents of that specific feature. Otherwise, all feature locations will be present. Feature "+
		"locations at the same tree level are listed in order of refStart."
)
public class ReferenceShowFeatureTreeCommand extends ReferenceSequenceModeCommand<ReferenceFeatureTreeResult> {

	private static final String FEATURE_NAME = "featureName";
	
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
	}

	@Override
	public ReferenceFeatureTreeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		Feature limitingFeature = null;
		if(featureName != null) {
			limitingFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		}
		boolean recursive = true;
		return refSeq.getFeatureTree(cmdContext, limitingFeature, recursive, true);
	}

	@CompleterClass
	public static class Completer extends FeatureLocNameCompleter {}

}
