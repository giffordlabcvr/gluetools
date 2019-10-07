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
package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.genotyping.BaseCladeCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class MaxLikelihoodCladeCategory extends BaseCladeCategory {

	public static final String DISTANCE_SCALING_EXPONENT = "distanceScalingExponent";
	public static final String DISTANCE_CUTOFF = "distanceCutoff";
	public static final String FINAL_CLADE_CUTOFF = "finalCladeCutoff";

	private Double distanceScalingExponent;
	private Double distanceCutoff;
	private Double finalCladeCutoff;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.distanceCutoff = PluginUtils.configureDoubleProperty(configElem, DISTANCE_CUTOFF, 0.0, false, null, false, true);
		this.finalCladeCutoff = PluginUtils.configureDoubleProperty(configElem, FINAL_CLADE_CUTOFF, 50.0, false, 100.0, true, true);
		this.distanceScalingExponent = PluginUtils.configureDoubleProperty(configElem, DISTANCE_SCALING_EXPONENT, null, false, 0.0, false, true);
	}

	public Double getDistanceScalingExponent() {
		return distanceScalingExponent;
	}

	public Double getDistanceCutoff() {
		return distanceCutoff;
	}

	public Double getFinalCladeCutoff() {
		return finalCladeCutoff;
	}
	
	
}
