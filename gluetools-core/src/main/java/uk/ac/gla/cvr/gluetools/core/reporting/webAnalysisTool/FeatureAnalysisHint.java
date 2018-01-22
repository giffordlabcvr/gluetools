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
package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class FeatureAnalysisHint implements Plugin {

	public static final String FEATURE_NAME = "featureName";
	public static final String INCLUDES_SEQUENCE_CONTENT = "includesSequenceContent";
	public static final String DERIVE_SEQUENCE_CONTENT_FROM = "deriveSequenceAnalysisFrom";
	public static final String VARIATION_SCAN_HINT = "variationScanHint";
	
	private String featureName;
	private Boolean includesSequenceContent;
	private String deriveSequenceAnalysisFrom;
	private List<VariationScanHint> variationScanHints;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		includesSequenceContent = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, INCLUDES_SEQUENCE_CONTENT, false)).orElse(false);
		deriveSequenceAnalysisFrom = PluginUtils.configureStringProperty(configElem, DERIVE_SEQUENCE_CONTENT_FROM, false);
		variationScanHints = 
				PluginFactory.createPlugins(pluginConfigContext, VariationScanHint.class, 
						GlueXmlUtils.getXPathElements(configElem, VARIATION_SCAN_HINT));

	}

	public String getFeatureName() {
		return featureName;
	}

	public Boolean getIncludesSequenceContent() {
		return includesSequenceContent;
	}

	public String getDeriveSequenceAnalysisFrom() {
		return deriveSequenceAnalysisFrom;
	}

	public List<VariationScanHint> getVariationScanHints() {
		return variationScanHints;
	}
	
}
