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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"show", "feature-segments"}, 
		description = "Show the homology for a specific feature relative to a specific reference sequence", 
		docoptUsages = { "-r <relRefName> -f <featureName> [-q]"},
		docoptOptions={
				"-r <relRefName>, --relRefName <relRefName>     Related reference",
				"-f <featureName>, --featureName <featureName>  Feature to translate",
				"-q, --queryAlignedSegments                     Return list of queryAlignedSegment objects",
		},
		furtherHelp = 
		"If this member is in a constrained alignment, the <relRefName> argument names a reference "+
		"sequence constraining an ancestor alignment of this member's alignment. "+
		"If this member is in an unconstrained alignment, the <relRefName> argument names a reference "+
		"sequence which is a member of the same alignment. "+
		"The <featureName> argument names a feature location which is defined on the named reference. "+
		"Together these specify a feature location, the result of the command shows which parts of the member sequence are "+
		"homologous to this feature-location, according to the alignment",
		metaTags = {}	
)
public class MemberShowFeatureSegmentsCommand extends MemberModeCommand<CommandResult> {


	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	private static final String QUERY_ALIGNED_SEGMENTS = "queryAlignedSegments";
	
	private String relRefName;
	private String featureName;
	private boolean queryAlignedSegments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.queryAlignedSegments = PluginUtils.configureBooleanProperty(configElem, QUERY_ALIGNED_SEGMENTS, true);
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToRelatedRefSegs = alignment.translateToRelatedRef(cmdContext, memberToAlmtSegs, relatedRef);
		

		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegs = ReferenceSegment.intersection(memberToRelatedRefSegs, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		
		List<QueryAlignedSegment> memberToFeatureLocRefSegsMerged = QueryAlignedSegment.mergeAbutting(memberToFeatureLocRefSegs, 
				QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
				QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

		if(queryAlignedSegments) {
			return new QueryAlignedSegment.QueryAlignedSegmentsResult(memberToFeatureLocRefSegsMerged);
		} else {
			return new MemberQueryAlignedSegmentsTableResult(memberToFeatureLocRefSegsMerged);
		}
	}

	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {}

	
}
