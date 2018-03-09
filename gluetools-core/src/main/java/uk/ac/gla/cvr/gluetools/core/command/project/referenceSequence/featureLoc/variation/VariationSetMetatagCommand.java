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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.feature.FeatureModeCommand.MetatagTypeCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "metatag"},
		docoptUsages={"[-C] <metatagName> <metatagValue>"},
		docoptOptions={
				"-C, --noCommit                                 Don't commit to the database [default: false]",
		},
		metaTags={CmdMeta.updatesDatabase},
		description="Add or update a metatag with a certain name/value",
		furtherHelp="Metatags are metadata for variations."
	) 
public class VariationSetMetatagCommand extends VariationModeCommand<OkResult> {

	public static final String NO_COMMIT = "noCommit";
	public static final String METATAG_NAME = "metatagName";
	public static final String METATAG_VALUE = "metatagValue";

	private Boolean noCommit;
	private VariationMetatag.VariationMetatagType metatagType;
	private String metatagValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		metatagType = PluginUtils.configureEnumProperty(VariationMetatag.VariationMetatagType.class, configElem, METATAG_NAME, true);
		metatagValue = PluginUtils.configureStringProperty(configElem, METATAG_VALUE, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		Map<String, String> metatagPkMap = VariationMetatag.pkMap(variation.getFeatureLoc().getReferenceSequence().getName(), 
				variation.getFeatureLoc().getFeature().getName(),
				variation.getName(),
				metatagType.name());
		VariationMetatag variationMetatag = 
				GlueDataObject.lookup(cmdContext, VariationMetatag.class, metatagPkMap, true);
		if(variationMetatag == null) {
			variationMetatag = 
					GlueDataObject.create(cmdContext, VariationMetatag.class, metatagPkMap, false);
			variationMetatag.setVariation(variation);
		}
		variationMetatag.setValue(metatagValue);
		if(noCommit) {
			cmdContext.cacheUncommitted(variationMetatag);
		} else {
			cmdContext.commit();
		}
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}
