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
package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"unset", "metatag"},
		docoptUsages={"<metatagName>"},
		metaTags = {},
		description="Specify that this feature does not have a metatag",
		furtherHelp="This command succeeds if the feature already does not have the metatag."
	) 
public class FeatureUnsetMetatagCommand extends FeatureModeCommand<DeleteResult> {

	public static final String METATAG_NAME = "metatagName";
	private FeatureMetatag.FeatureMetatagType metatagType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		metatagType = PluginUtils.configureEnumProperty(FeatureMetatag.FeatureMetatagType.class, configElem, METATAG_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		DeleteResult result = GlueDataObject.delete(cmdContext, 
				FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}