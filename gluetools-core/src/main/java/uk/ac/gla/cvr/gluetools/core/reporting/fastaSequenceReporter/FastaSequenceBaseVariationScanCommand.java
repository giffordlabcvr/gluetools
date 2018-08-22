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
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanMatchesAsDocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanMatchesAsTableResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public abstract class FastaSequenceBaseVariationScanCommand extends FastaSequenceReporterCommand<CommandResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";
	public static final String EXCLUDE_INSUFFICIENT_COVERAGE = "excludeInsufficientCoverage";


	private Expression whereClause;
	private Boolean descendentFeatures;
	private Boolean excludeAbsent;
	private Boolean excludeInsufficientCoverage;
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
		this.excludeInsufficientCoverage = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_INSUFFICIENT_COVERAGE, false)).orElse(false);
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected CommandResult executeAux(
			CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter, String fastaID,
			DNASequence fastaNTSeq, String targetRefName, List<QueryAlignedSegment> establishedQueryToTargetRefSegs) {
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());
		Alignment linkingAlmt = linkingAlmtMember.getAlignment();

		ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelRefName());

		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}
		
		Class<? extends VariationScannerMatchResult> matchResultClass = null;
		if(variationScanRenderHints.showMatchesAsTable()) {
			matchResultClass = VariationScanUtils.getMatchResultClass(cmdContext, relatedRef, featuresToScan, whereClause);
		}
		
		List<QueryAlignedSegment> queryToTargetRefSegs;
		if(establishedQueryToTargetRefSegs == null) {
			// align query to target reference
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, fastaSequenceReporter.getAlignerModuleName());
			AlignerResult alignerResult = aligner.computeConstrained(cmdContext, targetRef.getName(), fastaID, fastaNTSeq);

			// extract segments from aligner result
			queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);
		} else {
			queryToTargetRefSegs = establishedQueryToTargetRefSegs;
		}
		
		
		// translate segments to linking alignment coordinate space
		List<QueryAlignedSegment> queryToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
				linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		
		
		for(Feature featureToScan: featuresToScan) {
			FeatureLocation featureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
							FeatureLocation.pkMap(relatedRef.getName(), featureToScan.getName()), true);
			if(featureLoc == null) {
				continue;
			}
			List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
			List<QueryAlignedSegment> queryToRelatedRefSegs = linkingAlmt.translateToRelatedRef(cmdContext, queryToLinkingAlmtSegs, relatedRef);

			String fastaNTs = fastaNTSeq.getSequenceAsString();

			List<NtQueryAlignedSegment> queryToRelatedRefNtSegs =
					queryToRelatedRefSegs.stream()
					.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
							SegmentUtils.base1SubString(fastaNTs, seg.getQueryStart(), seg.getQueryEnd())))
							.collect(Collectors.toList());

			variationScanResults.addAll(FeatureLocation.variationScan(cmdContext, queryToRelatedRefNtSegs, fastaNTs, null, variationsToScan, excludeAbsent, excludeInsufficientCoverage));

		}
		
		VariationScanResult.sortVariationScanResults(variationScanResults);
		
		if(variationScanRenderHints.showMatchesAsTable()) {
			return new VariationScanMatchesAsTableResult(matchResultClass, variationScanResults);
		} else if(variationScanRenderHints.showMatchesAsDocument()) {
			return new VariationScanMatchesAsDocumentResult(variationScanResults);
		} else {
			return new VariationScanCommandResult(variationScanResults);
		}
	}
	
}
