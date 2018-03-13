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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.VariationScanMatchCommandResult;
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
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public abstract class FastaSequenceBaseVariationScanCommand extends FastaSequenceReporterCommand<CommandResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";
	public static final String EXCLUDE_INSUFFICIENT_COVERAGE = "excludeInsufficientCoverage";


	private Expression whereClause;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	private Boolean excludeAbsent;
	private Boolean excludeInsufficientCoverage;
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
		this.excludeInsufficientCoverage = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_INSUFFICIENT_COVERAGE, false)).orElse(false);
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected CommandResult executeAux(
			CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter, String fastaID,
			DNASequence fastaNTSeq, String targetRefName) {
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName());
		Alignment tipAlmt = tipAlmtMember.getAlignment();

		ReferenceSequence ancConstrRef = tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName());

		List<ReferenceSequence> refsToScan;
		if(multiReference) {
			refsToScan = tipAlmt.getAncestorPathReferences(cmdContext, getAcRefName());
			if(!refsToScan.contains(targetRef)) {
				refsToScan.add(0, targetRef);
			}
		} else {
			refsToScan = Arrays.asList(ancConstrRef);
		}
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}
		
		Class<? extends VariationScannerMatchResult> matchResultClass = null;
		if(variationScanRenderHints.isShowMatchesSeparately()) {
			matchResultClass = VariationScanUtils.getMatchResultClass(cmdContext, refsToScan, featuresToScan, whereClause);
		}
		
		// align query to target reference
		Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, fastaSequenceReporter.getAlignerModuleName());
		AlignerResult alignerResult = aligner.computeConstrained(cmdContext, targetRef.getName(), fastaID, fastaNTSeq);

		// extract segments from aligner result
		List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		// translate segments to tip alignment reference
		List<QueryAlignedSegment> queryToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		
		VariationScanUtils.visitVariations(cmdContext, refsToScan, featuresToScan, whereClause, new VariationScanUtils.VariationConsumer() {
			@Override
			public void consumeVariations(ReferenceSequence refToScan,
					FeatureLocation featureLoc, List<Variation> variationsToScan) {
				// translate segments to scanned reference
				List<QueryAlignedSegment> queryToScannedRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, queryToTipAlmtRefSegs, refToScan);
				
				// trim query to scanned ref segs down to the feature area.
				List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
				List<QueryAlignedSegment> queryToScannedRefSegs = 
						ReferenceSegment.intersection(queryToScannedRefSegsFull, featureLocRefSegs, ReferenceSegment.cloneLeftSegMerger());
		
				String fastaNTs = fastaNTSeq.getSequenceAsString();
		
				List<NtQueryAlignedSegment> queryToScannedRefNtSegs =
						queryToScannedRefSegs.stream()
						.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
								SegmentUtils.base1SubString(fastaNTs, seg.getQueryStart(), seg.getQueryEnd())))
								.collect(Collectors.toList());
		
				variationScanResults.addAll(featureLoc.variationScan(cmdContext, queryToScannedRefNtSegs, variationsToScan, excludeAbsent, excludeInsufficientCoverage));
		
			}
		});
		
		VariationScanResult.sortVariationScanResults(variationScanResults);
		
		if(variationScanRenderHints.isShowMatchesSeparately()) {
			return new VariationScanMatchCommandResult(matchResultClass, variationScanResults);
		} else {
			return new VariationScanCommandResult(variationScanResults);
		}
	}
	
}
