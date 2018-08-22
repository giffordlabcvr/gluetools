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
package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasDisplayName;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequenceException.Code;

@GlueDataClass(
		defaultObjectRendererFtlFile = "defaultRenderers/reference.ftlx",
		defaultListedProperties = {_ReferenceSequence.NAME_PROPERTY, ReferenceSequence.SEQ_SOURCE_NAME_PATH, ReferenceSequence.SEQ_ID_PATH}, 
		listableBuiltInProperties = {_ReferenceSequence.NAME_PROPERTY, _ReferenceSequence.DISPLAY_NAME_PROPERTY, ReferenceSequence.SEQ_SOURCE_NAME_PATH, ReferenceSequence.SEQ_ID_PATH}, 
		modifiableBuiltInProperties = {_ReferenceSequence.DISPLAY_NAME_PROPERTY})
public class ReferenceSequence extends _ReferenceSequence implements HasDisplayName {

	public static final String SEQ_SOURCE_NAME_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQ_ID_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;


	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}


	public void validate(CommandContext cmdContext) {
		getFeatureLocations().forEach(featureLoc -> featureLoc.validate(cmdContext));

	}


	public ReferenceFeatureTreeResult getFeatureTree(CommandContext cmdContext, Feature limitingFeature, boolean recursive, boolean includeLabeledCodons) {
		ReferenceFeatureTreeResult featureTree = new ReferenceFeatureTreeResult(getName(), getRenderedName());
		buildTree(cmdContext, limitingFeature, recursive, includeLabeledCodons, featureTree);
		return featureTree;
	}

	public void buildTree(CommandContext cmdContext,
			Feature limitingFeature, boolean recursive, boolean includeLabeledCodons, ReferenceFeatureTreeResult featureTree) {
		List<FeatureLocation> featureLocations = new ArrayList<FeatureLocation>(getFeatureLocations());
		Collections.sort(featureLocations, new FeatureLocationComparator());
		for(FeatureLocation featureLocation: featureLocations) {
			Feature feature = featureLocation.getFeature();
			if(limitingFeature == null || 
					feature.getName().equals(limitingFeature.getName()) ||
					(recursive && feature.isDescendentOf(limitingFeature)) || 
					limitingFeature.isDescendentOf(feature) ) {
				featureTree.addFeatureLocation(cmdContext, featureLocation, includeLabeledCodons);
			}
 		}
	}


	// sort feature locations by start NT index, adding those which have segments defined before those that don't.
	// if two feature locations start at the same NT index, add first the feature location higher up the feature tree, 
	// breaking ties by feature name.
	// if neither feature location has a segment defined, sort by feature name.
	private class FeatureLocationComparator implements Comparator<FeatureLocation> {
		@Override
		public int compare(FeatureLocation o1, FeatureLocation o2) {
			List<FeatureSegment> o1Segs = o1.getSegments();
			List<FeatureSegment> o2Segs = o2.getSegments();
			if(o1Segs.size() > 0) {
				if(o2Segs.size() > 0) {
					int refStartComparison = Integer.compare(o1Segs.get(0).getRefStart(), o2Segs.get(0).getRefStart());
					if(refStartComparison != 0) {
						return refStartComparison;
					}
					int featureDepthComparison = Integer.compare(o1.getFeature().getDepthInTree(), o2.getFeature().getDepthInTree());
					if(featureDepthComparison != 0) {
						return featureDepthComparison;
					}
					return o1.getFeature().getName().compareTo(o2.getFeature().getName());
				} else {
					return -1;
				}
			} else {
				if(o2Segs.size() > 0) {
					return 1;
				} else {
					return o1.getFeature().getName().compareTo(o2.getFeature().getName());
				} 
			}
		}
	}

	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		if(glueConfigContext.getIncludeVariations()) {
			for(FeatureLocation featureLoc: getFeatureLocations()) {
				StringBuffer featureLocConfigBuf = new StringBuffer();
				featureLoc.generateGlueConfig(indent+INDENT, featureLocConfigBuf, glueConfigContext);
				if(featureLocConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("feature-location ").append(featureLoc.getFeature().getName()).append("\n");
					glueConfigBuf.append(featureLocConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit\n");
				}
			}
			
		}
	}
	

	// get membership of a specific linking alignment
	public AlignmentMember getLinkingAlignmentMembership(String linkingAlmtName) {
		return getSequence().getAlignmentMemberships().stream()
				.filter(am -> am.getAlignment().getName().equals(linkingAlmtName))
				.findFirst()
				.orElseThrow(() -> new ReferenceSequenceException(
						Code.REFERENCE_SEQUENCE_NOT_MEMBER_OF_ALIGNMENT, getName(), linkingAlmtName));
	}

	
}
