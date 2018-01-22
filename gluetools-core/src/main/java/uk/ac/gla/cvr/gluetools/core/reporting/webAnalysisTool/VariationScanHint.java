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

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class VariationScanHint implements Plugin {

	public static final String REFERENCE_SEQUENCE = "referenceSequence";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	
	private String referenceName;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_SEQUENCE, true);
		multiReference = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		descendentFeatures = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	}

	public String getReferenceName() {
		return referenceName;
	}

	public Boolean getMultiReference() {
		return multiReference;
	}

	public Boolean getDescendentFeatures() {
		return descendentFeatures;
	}

}
