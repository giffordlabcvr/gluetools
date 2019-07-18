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
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "metatag"},
		docoptUsages={"<metatagName> <metatagValue>"},
		metaTags = {},
		description="Add or update a metatag with a certain name/value",
		furtherHelp="Metatags are metadata for features."
	) 
public class FeatureSetMetatagCommand extends FeatureModeCommand<OkResult> {

	public static final String METATAG_NAME = "metatagName";
	public static final String METATAG_VALUE = "metatagValue";

	private FeatureMetatag.FeatureMetatagType metatagType;
	private String metatagValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		metatagType = PluginUtils.configureEnumProperty(FeatureMetatag.FeatureMetatagType.class, configElem, METATAG_NAME, true);
		metatagValue = PluginUtils.configureStringProperty(configElem, METATAG_VALUE, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		FeatureMetatag featureMetatag = 
				GlueDataObject.lookup(cmdContext, 
						FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), true);
		if(featureMetatag == null) {
			featureMetatag = GlueDataObject.create(cmdContext, 
					FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), false);
			featureMetatag.setFeature(feature);
		}
		featureMetatag.setValue(metatagValue);
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}
