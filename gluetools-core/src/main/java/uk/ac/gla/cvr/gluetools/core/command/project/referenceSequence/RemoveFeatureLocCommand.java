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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"remove", "feature-location"}, 
	docoptUsages={"[-r] <featureName>"},
	docoptOptions={"-r, --recursive  Also remove locations of descendent features"},
	metaTags={CmdMeta.updatesDatabase},
	description="Remove a feature location") 
public class RemoveFeatureLocCommand extends ReferenceSequenceModeCommand<DeleteResult> {

	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";

	private String featureName;
	private Boolean recursive;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		List<String> locsToDelete = new ArrayList<String>();
		locsToDelete.add(featureName);
		if(recursive) {
			List<Feature> descendents = feature.getDescendents();
			locsToDelete.addAll(descendents.stream().map(Feature::getName).collect(Collectors.toList()));
		}
		int deleted = 0;
		for(String locToDelete: locsToDelete) {
			DeleteResult result = GlueDataObject.delete(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRefSeqName(), locToDelete), true);
			cmdContext.commit();
			deleted += result.getNumber();
		}
		return new DeleteResult(FeatureLocation.class, deleted);
	}

	@CompleterClass
	public static class Completer extends FeatureLocNameCompleter {}

}
