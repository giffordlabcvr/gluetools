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
package uk.ac.gla.cvr.gluetools.core.genotyping.simpledistance;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.genotyping.BaseCladeCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class SimpleDistanceCladeCategory extends BaseCladeCategory {

	public static final String MAX_DISTANCE = "maxDistance";
	public static final String USE_INTERNAL_DISTANCE = "useInternalDistance";
	public static final String MAX_INTERNAL_DISTANCE = "maxInternalDistance";
	public static final String MIN_PLACEMENT_PERCENTAGE = "minPlacementPercentage";

	// If the query is within <maxDistance> patristic distance of a reference sequence in clade X, 
	// then the query may be assigned to clade X. If multiple clades within a clade category meet this 
	// criterion, the clade with the nearest distance wins.
	private Double maxDistance;
	// if <useInternalDistance> is true, a query may be assigned to clade X if it is internal to clade X
	// (i.e. its grandparent internal node is a descendent of the node that defines X) based on a different
	// distance threshold
	private Boolean useInternalDistance;
	// If <useInternalDistance> is true and the query is within <maxInternalDistance> patristic distance 
	// of a reference sequence in clade X, *and* it is internal to clade X, then it may be assigned to clade X. 
	// If <useInternalDistance> is true and <maxInternalDistance> is null (the default) then being internal
	// to clade X is enough to assign the query to clade X. 
	
	private Double maxInternalDistance;
	// The maxLikelihoodPlacer generates multiple placements, each with its own likelihood weight ratio percentage.
	// Separate clade assignments are generated for each clade category for each placment. 
	// The <minPlacementPercentage> option indicates the minimum total percentage accrued by a candidate assignment
	// across multiple placements for that assignment to be made by the genotyper as a whole.
	// If multiple candidate assignments meet this threshold, then no assignment is made.
	private Double minPlacementPercentage;

	// Examples:

	// Setting <useInternalDistance> to true, <maxInternalDistance> to null and <maxDistance> to 0.0
	// gives a purely topological assignment criterion

	// Setting <useInternalDistance> to false, <maxInternalDistance> to null and <maxDistance> to 0.5
	// gives a purely distance-based assignment criterion

	// Setting <useInternalDistance> to true, <maxInternalDistance> to 0.8 and <maxDistance> to 0.3
	// gives a hybrid assignment criterion

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.maxDistance = 
				PluginUtils.configureDoubleProperty(configElem, MAX_DISTANCE, 0.0, true, null, false, true);
		this.useInternalDistance = 
				Optional.ofNullable(
						PluginUtils.configureBooleanProperty(configElem, USE_INTERNAL_DISTANCE, false))
				.orElse(false);
		this.maxInternalDistance = 
				Optional.ofNullable(
						PluginUtils.configureDoubleProperty(configElem, MAX_INTERNAL_DISTANCE, 0.0, false, null, false, false))
				.orElse(null);
		this.minPlacementPercentage = 
				Optional.ofNullable(
						PluginUtils.configureDoubleProperty(configElem, MIN_PLACEMENT_PERCENTAGE, 0.0, false, 100.0, true, false))
				.orElse(50.0);
	}

	public Double getMaxDistance() {
		return maxDistance;
	}

	public Double getMaxInternalDistance() {
		return maxInternalDistance;
	}

	public Double getMinPlacementPercentage() {
		return minPlacementPercentage;
	}

	public boolean useInternalDistance() {
		return useInternalDistance;
	}

	
	
}
