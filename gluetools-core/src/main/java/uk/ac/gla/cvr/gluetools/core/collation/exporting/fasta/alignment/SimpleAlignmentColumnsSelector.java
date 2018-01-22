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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AminoAcidRegionSelector;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.NucleotideRegionSelector;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class SimpleAlignmentColumnsSelector implements IAlignmentColumnsSelector {

	private String relatedRefName;
	private String featureName;
	
	private Integer ntStart;
	private Integer ntEnd;
	private String lcStart;
	private String lcEnd;
	
	public SimpleAlignmentColumnsSelector(String relatedRefName,
			String featureName, Integer ntStart, Integer ntEnd, String lcStart,
			String lcEnd) {
		super();
		this.relatedRefName = relatedRefName;
		this.featureName = featureName;
		this.ntStart = ntStart;
		this.ntEnd = ntEnd;
		this.lcStart = lcStart;
		this.lcEnd = lcEnd;
	}

	@Override
	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext) {
		if(lcStart == null && lcEnd == null && ntStart == null && ntEnd == null) {
			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName));
			List<ReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments();
			return featureRefSegs;
		}
		if((lcStart != null || lcEnd != null) && (ntStart != null || ntEnd != null)) {
			throw new RuntimeException("Cannot specify both labelledCodon and ntRegion");
		}
		if(lcStart != null || lcEnd != null) {
			return AminoAcidRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName, featureName, lcStart, lcEnd);
		}
		if(ntStart != null || ntEnd != null) {
			return NucleotideRegionSelector.selectAlignmentColumns(cmdContext, relatedRefName, featureName, ntStart, ntEnd);
		}
		throw new RuntimeException("Badly specified labelledCodon / ntRegion");
	}

	@Override
	public String getRelatedRefName() {
		return relatedRefName;
	}

	public String getFeatureName() {
		return featureName;
	}
	
	
	
}
