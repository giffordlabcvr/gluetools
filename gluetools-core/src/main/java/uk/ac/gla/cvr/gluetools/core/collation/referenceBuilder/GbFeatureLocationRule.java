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

import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.AddFeatureLocCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.AddFeatureSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class GbFeatureLocationRule implements Plugin {

	private String featureName;
	private Pattern gbFeatureKeyPattern;
	private Pattern gbQualifierNamePattern;
	private Pattern gbQualifierValuePattern;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		featureName = PluginUtils.configureString(configElem, "@featureName", true);
		gbFeatureKeyPattern = PluginUtils.configureRegexPatternProperty(configElem, "gbFeatureKeyPattern", false);
		gbQualifierNamePattern = PluginUtils.configureRegexPatternProperty(configElem, "gbQualifierNamePattern", false);
		gbQualifierValuePattern = PluginUtils.configureRegexPatternProperty(configElem, "gbQualifierValuePattern", false);
	}

	public boolean run(CommandContext cmdContext, Element featureElem, GbRefBuilder gbRefBuilder) {
		String featureKey = GlueXmlUtils.getXPathString(featureElem, "GBFeature_key/text()");
		if(gbFeatureKeyPattern != null && !gbFeatureKeyPattern.matcher(featureKey).find()) {
			return false;
		}
		List<Element> qualifierElems = GlueXmlUtils.getXPathElements(featureElem, "GBFeature_quals/GBQualifier");
		boolean matchFound = false;
		String qualifierName = null, qualifierValue = null;
		
		if(gbQualifierNamePattern != null && gbQualifierValuePattern != null) {
			for(Element qualifierElem: qualifierElems) {
				qualifierName = GlueXmlUtils.getXPathString(qualifierElem, "GBQualifier_name/text()");
				if(!gbQualifierNamePattern.matcher(qualifierName).find()) {
					continue;
				}
				qualifierValue = GlueXmlUtils.getXPathString(qualifierElem, "GBQualifier_value/text()");
				if(!gbQualifierValuePattern.matcher(qualifierValue).find()) {
					continue;
				}
				matchFound = true;
				break;
			}
		} else {
			matchFound = true;
		}
		if(!matchFound) {
			return false;
		}
		
		gbRefBuilder.log("Creating feature location "+featureName+" from GB feature with featureKey:"+featureKey);
		if(qualifierName != null && qualifierValue != null) {
			gbRefBuilder.log("Matching qualifier name:"+qualifierName+", value:"+qualifierValue);
		}
		cmdContext.cmdBuilder(AddFeatureLocCommand.class)
			.set(AddFeatureLocCommand.FEATURE_NAME, featureName)
			.execute();
		try(ModeCloser featureLocMode = cmdContext.pushCommandMode("feature-location", featureName)) {
			List<Element> intervalElems = GlueXmlUtils.getXPathElements(featureElem, "GBFeature_intervals/GBInterval");
			Integer lastTo = null;
			for(Element intervalElem: intervalElems) {
				Integer from = Integer.parseInt(GlueXmlUtils.getXPathString(intervalElem, "GBInterval_from/text()"));
				if(lastTo != null && from.equals(lastTo)) {
					from = from+1; // hack to work around this kind of thing: join(1801..2082,2082..2546)
				}
				Integer to = Integer.parseInt(GlueXmlUtils.getXPathString(intervalElem, "GBInterval_to/text()"));
				gbRefBuilder.log("Adding segment from GB interval: ["+from+", "+to+"]");
				cmdContext.cmdBuilder(AddFeatureSegmentCommand.class)
				.set(AddFeatureSegmentCommand.REF_START, from)
				.set(AddFeatureSegmentCommand.REF_END, to)
				.execute();
				lastTo = to;
			}
		}
		return true;
	}

	public String getFeatureName() {
		return featureName;
	}

	
}
