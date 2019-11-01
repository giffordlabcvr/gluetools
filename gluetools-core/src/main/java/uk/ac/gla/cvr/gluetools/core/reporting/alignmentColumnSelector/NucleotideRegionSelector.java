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
package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FeatureReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="nucleotideRegionSelector")
public class NucleotideRegionSelector extends RegionSelector {

	private Integer startNt;
	private Integer endNt;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.startNt = PluginUtils.configureIntProperty(configElem, "startNt", false);
		this.endNt = PluginUtils.configureIntProperty(configElem, "endNt", false);
	}

	@Override
	protected List<FeatureReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName) {
		return selectAlignmentColumns(cmdContext, relRefName, getFeatureName(), startNt, endNt);
	}

	public static List<FeatureReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName, String featureName, Integer startNt, Integer endNt) {
		int startNtToUse;
		int endNtToUse;
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));
		List<FeatureReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments().stream()
				.map(rs -> new FeatureReferenceSegment(featureName, rs.getRefStart(), rs.getRefEnd()))
				.collect(Collectors.toList());
		
		if(startNt != null) {
			startNtToUse = startNt;
		} else {
			startNtToUse = ReferenceSegment.minRefStart(featureRefSegs);
		}
		if(endNt != null) {
			endNtToUse = endNt;
		} else {
			endNtToUse = ReferenceSegment.maxRefEnd(featureRefSegs);
		}
		List<FeatureReferenceSegment> result = Arrays.asList(new FeatureReferenceSegment(featureName, startNtToUse, endNtToUse));
		if(featureRefSegs != null) {
			result = ReferenceSegment.intersection(featureRefSegs, result, ReferenceSegment.cloneLeftSegMerger());
		}
		return result;
	}

	public void setStartNt(Integer startNt) {
		this.startNt = startNt;
	}

	public void setEndNt(Integer endNt) {
		this.endNt = endNt;
	}

	
	
}
