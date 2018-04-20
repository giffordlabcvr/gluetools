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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a member sequence for variations", 
		docoptUsages = { "-r <relRefName> [-m] -f <featureName> [-d] [-w <whereClause>] [-e] [-i] [-v | -o]" },
		docoptOptions = { 
		"-r <relRefName>, --relRefName <relRefName>     Related reference",
		"-m, --multiReference                           Scan across references",
		"-f <featureName>, --featureName <featureName>  Feature to scan",
		"-d, --descendentFeatures                       Include descendent features",
		"-w <whereClause>, --whereClause <whereClause>  Qualify variations",
		"-e, --excludeAbsent                            Exclude absent variations",
		"-i, --excludeInsufficientCoverage              Exclude where insufficient coverage",
		"-v, --showMatchesAsTable                       Table with one row per match",
		"-o, --showMatchesAsDocument                    Document with one object per match",
		},
		furtherHelp = 
		"The <relRefName> argument names a reference sequence constraining an ancestor alignment of this alignment (if constrained), "+
		"or simply a reference which is a member of this alignment (if unconstrained). "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between the containing alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> argument names a feature location which is defined on this reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
		"The result will be confined to this feature location. "+
		"The <whereClause>, if present, qualifies the set of variations scanned for. "+
		"If --excludeAbsent is used, variations which were confirmed to be absent will not appear in the results. "+
		"If --excludeInsufficientCoverage is used, variations for which the query did not sufficiently cover the scanned "+
		"area will not appear in the results. "+
		"If --showMatchesAsTable is used, a table is returned with one row for each individual match. In this case the "+
		"selected variations must all be of the same type. "+
		"If --showMatchsAsDocument is used, a document is returned with an object for each individual match.",
		metaTags = {}	
)
public class MemberVariationScanCommand extends MemberModeCommand<CommandResult> {

	public static final String REL_REF_NAME = "relRefName";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String FEATURE_NAME = "featureName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";
	public static final String EXCLUDE_INSUFFICIENT_COVERAGE = "excludeInsufficientCoverage";

	private String relRefName;
	private String featureName;
	private Boolean descendentFeatures;
	private Expression whereClause;
	private Boolean multiReference;
	private Boolean excludeAbsent;
	private Boolean excludeInsufficientCoverage;
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
		this.excludeInsufficientCoverage = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_INSUFFICIENT_COVERAGE, false)).orElse(false);
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));

		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();

		List<ReferenceSequence> refsToScan;
		if(multiReference) {
			if(!alignment.isConstrained()) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --multiReference option can only be used with constrained alignments");
			}
			refsToScan = alignment.getAncestorPathReferences(cmdContext, relRefName);
		} else {
			refsToScan = Arrays.asList(alignment.getRelatedRef(cmdContext, relRefName));
		}
		
		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}
		Class<? extends VariationScannerMatchResult> matchResultClass = null;
		if(variationScanRenderHints.showMatchesAsTable()) {
			matchResultClass = VariationScanUtils.getMatchResultClass(cmdContext, refsToScan, featuresToScan, whereClause);
		}
		
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		VariationScanUtils.visitVariations(cmdContext, refsToScan, featuresToScan, whereClause, new VariationScanUtils.VariationConsumer() {
			@Override
			public void consumeVariations(ReferenceSequence refToScan,
					FeatureLocation featureLoc, List<Variation> variationsToScan) {
				variationScanResults.addAll(memberVariationScan(cmdContext, almtMember, refToScan, featureLoc, variationsToScan, 
						excludeAbsent, excludeInsufficientCoverage));
			}
		});

		VariationScanResult.sortVariationScanResults(variationScanResults);
		if(variationScanRenderHints.showMatchesAsTable()) {
			return new VariationScanMatchesAsTableResult(matchResultClass, variationScanResults);
		} else if(variationScanRenderHints.showMatchesAsDocument()) {
			return new VariationScanMatchesAsDocumentResult(variationScanResults);
		} else {
			return new VariationScanCommandResult(variationScanResults);
		}
	}

	public static List<VariationScanResult<?>> memberVariationScan(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence relatedRef, FeatureLocation featureLoc,
			List<Variation> variationsToScan, boolean excludeAbsent, boolean excludeInsufficientCoverage) {
		Alignment alignment = almtMember.getAlignment();
		
		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToRelatedRefRefSegs = alignment.translateToRelatedRef(cmdContext, memberToAlmtSegs, relatedRef);

		AbstractSequenceObject memberSeqObj = almtMember.getSequence().getSequenceObject();
		
		String memberNts = memberSeqObj.getNucleotides(cmdContext);
		
		List<NtQueryAlignedSegment> memberToRelatedRefNtSegs = 
				memberToRelatedRefRefSegs.stream()
				.map(seg -> new NtQueryAlignedSegment(
						seg.getRefStart(), seg.getRefEnd(), 
						seg.getQueryStart(), seg.getQueryEnd(), 
						SegmentUtils.base1SubString(memberNts, seg.getQueryStart(), seg.getQueryEnd())))
				.collect(Collectors.toList());
		
		
		List<VariationScanResult<?>> variationScanResults = featureLoc.
				variationScan(cmdContext, memberToRelatedRefNtSegs, memberNts, variationsToScan, excludeAbsent, excludeInsufficientCoverage);
		VariationScanResult.sortVariationScanResults(variationScanResults);

		return variationScanResults;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {}

	
}
