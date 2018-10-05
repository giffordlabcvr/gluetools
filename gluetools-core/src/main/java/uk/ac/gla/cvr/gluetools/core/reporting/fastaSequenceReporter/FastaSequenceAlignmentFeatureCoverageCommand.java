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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"alignment-feature-coverage"}, 
		description = "Given a precomputed alignment with the target reference, show the coverage percentage for a specific feature location", 
		docoptUsages = {},
		docoptOptions = {},
		metaTags = { CmdMeta.inputIsComplex }	
)
public class FastaSequenceAlignmentFeatureCoverageCommand extends FastaSequenceReporterCommand<FastaSequenceAlignmentFeatureCoverageResult> 
	implements ProvidedProjectModeCommand{

	public static final String QUERY_TO_TARGET_SEGS = "queryToTargetSegs";
	
	private List<QueryAlignedSegment> queryToTargetSegs = new ArrayList<QueryAlignedSegment>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		CommandDocument qaSegsCmdDoc = PluginUtils.configureCommandDocumentProperty(configElem, QUERY_TO_TARGET_SEGS, true);
		qaSegsCmdDoc.getArray("alignedSegment").getItems().forEach(item -> {
			queryToTargetSegs.add(new QueryAlignedSegment((CommandObject) item));
		});
	}

	@Override
	protected FastaSequenceAlignmentFeatureCoverageResult execute(CommandContext cmdContext, FastaSequenceReporter fastaSequenceReporter) {
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(getTargetRefName()));
		AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());
		Alignment linkingAlmt = linkingAlmtMember.getAlignment();

		ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelRefName());
		
		// translate segments to linking alignment coordinate space
		List<QueryAlignedSegment> queryToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
				linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
				queryToTargetSegs);
		
		// translate segments to related reference
		List<QueryAlignedSegment> queryToRelatedRefSegs = linkingAlmt.translateToRelatedRef(cmdContext, queryToLinkingAlmtSegs, relatedRef);

		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRelRefName(), getFeatureName()), false);

		
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		Integer featureLength = IReferenceSegment.totalReferenceLength(featureLocRefSegs);

		List<QueryAlignedSegment> queryToFeatureLocRefSegs = ReferenceSegment.intersection(queryToRelatedRefSegs, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());

		Double refNtCvrgPct = IQueryAlignedSegment.getReferenceNtCoveragePercent(queryToFeatureLocRefSegs, featureLength);
		
		return new FastaSequenceAlignmentFeatureCoverageResult(getRelRefName(), getFeatureName(), refNtCvrgPct);
	}

}
