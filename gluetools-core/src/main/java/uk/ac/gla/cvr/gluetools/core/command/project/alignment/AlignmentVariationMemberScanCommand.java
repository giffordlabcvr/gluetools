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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberVariationScanCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanMatchResultRow;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

@CommandClass(
		commandWords={"variation", "member", "scan"}, 
		description = "Scan members for a specific variation", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <relRefName> -f <featureName> -v <variationName> [-e] [-i] [-t | -o]" },
		docoptOptions = { 
		"-c, --recursive                                      Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>        Qualify members",
		"-r <relRefName>, --relRefName <relRefName>           Related reference",
		"-f <featureName>, --featureName <featureName>        Feature name",
		"-v <variationName>, --variationName <variationName>  Variation name",
		"-e, --excludeAbsent                                  Exclude members where absent",
		"-i, --excludeInsufficientCoverage                    Exclude where insufficient coverage",
		"-t, --showMatchesAsTable                             Table with one row per match",
		"-o, --showMatchesAsDocument                          Document with one object per match",
		},
		furtherHelp = 
		"The <relRefName> argument names a reference sequence constraining an ancestor alignment of this alignment (if constrained), "+
		"or simply a reference which is a member of this alignment (if unconstrained). "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"If --excludeAbsent is used, members where the variation was confirmed to be absent will not appear in the results. "+
		"If --excludeInsufficientCoverage is used, members which do not sufficiently cover the scanned "+
		"area for the variation will not appear in the results. "+
		"If --showMatchesAsTable is used, a table is returned with one row for each individual match. "+
		"If --showMatchsAsDocument is used, a document is returned with an object for each individual match.",
				metaTags = {}	
)
public class AlignmentVariationMemberScanCommand extends AlignmentModeCommand<CommandResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String VARIATION_NAME = "variationName";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";
	public static final String EXCLUDE_INSUFFICIENT_COVERAGE = "excludeInsufficientCoverage";


	private Boolean recursive;
	private Optional<Expression> whereClause;

	private String relRefName;
	private String featureName;
	private String variationName;
	private Boolean excludeAbsent;
	private Boolean excludeInsufficientCoverage;
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
		this.excludeInsufficientCoverage = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_INSUFFICIENT_COVERAGE, false)).orElse(false);
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		List<MemberVariationScanResult> membVarScanResults = alignmentMemberVariationScan(
				cmdContext, variationScanRenderHints, getAlignmentName(), relRefName, featureName, variationName,
				whereClause, recursive, excludeAbsent, excludeInsufficientCoverage);
		if(variationScanRenderHints.showMatchesAsTable()) {
			Variation variation = 
					GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(relRefName, featureName, variationName), false);
			Class<? extends VariationScannerMatchResult> matchResultClass = variation.getVariationType().getMatchResultClass();
			List<MemberVariationScannerMatchResult> membVarScannerMatchResults = membVarScanResultsToMatchResults(membVarScanResults);
			return new AlignmentVariationMemberScanMatchesAsTableResult(matchResultClass, membVarScannerMatchResults);
		} else if(variationScanRenderHints.showMatchesAsDocument()) {
			List<MemberVariationScanResult> mvsrsPresent = membVarScanResults.stream().filter(mvsr -> mvsr.getVariationScanResult().isPresent()).collect(Collectors.toList());
			return new AlignmentVariationMemberScanMatchesAsDocumentResult(mvsrsPresent);
		} else {
			return new AlignmentVariationMemberScanResult(membVarScanResults);
		}
	}

	public List<MemberVariationScannerMatchResult> membVarScanResultsToMatchResults(
			List<MemberVariationScanResult> membVarScanResults) {
		List<MemberVariationScannerMatchResult> membVarScannerMatchResults = new ArrayList<MemberVariationScannerMatchResult>();
		for(MemberVariationScanResult mvsr : membVarScanResults) {
			if(mvsr.getVariationScanResult().isPresent()) {
				for(VariationScannerMatchResult vsmr: mvsr.getVariationScanResult().getVariationScannerMatchResults()) {
					membVarScannerMatchResults.add(new MemberVariationScannerMatchResult(mvsr.getMemberPkMap(), vsmr));
				}
			}
		}
		return membVarScannerMatchResults;
	}

	public static List<MemberVariationScanResult> alignmentMemberVariationScan(
			CommandContext cmdContext, VariationScanRenderHints variationScanRenderHints, String alignmentName, 
			String relatedRefName, String featureName, String variationName,
			Optional<Expression> whereClause, boolean recursive,
			boolean excludeAbsent, boolean excludeInsufficientCoverage) {
		
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, alignmentName, recursive, whereClause);
		
		int batchSize = 500;
		int offset = 0;
		
		List<MemberVariationScanResult> membVsrList = new ArrayList<MemberVariationScanResult>();

		while(offset < totalMembers) {
			int lastBatchIndex = Math.min(offset+batchSize, totalMembers);

			Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), false);
			ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, relatedRefName);
			FeatureLocation scannedFeatureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, featureName), false);
			Variation variation = 
					GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(relatedRefName, featureName, variationName), false);

			GlueLogger.getGlueLogger().finest("Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause, offset, batchSize, batchSize);
			GlueLogger.getGlueLogger().finest("Scanning variation for members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			
			for(AlignmentMember almtMember: almtMembers) {
				List<VariationScanResult<?>> variationScanResults = 
						MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, relatedRef, 
								scannedFeatureLoc, Arrays.asList(variation), excludeAbsent, excludeInsufficientCoverage);
				variationScanResults.forEach(vsr -> membVsrList.add(new MemberVariationScanResult(almtMember, vsr)));
			}
			cmdContext.newObjectContext();
			offset = offset+batchSize;
		}
		GlueLogger.getGlueLogger().finest("Scanned variation for "+totalMembers+" members");
		cmdContext.newObjectContext();
		return membVsrList;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("variationName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("relRefName");
					String featureName = (String) bindings.get("featureName");
					FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), true);
					if(featureLoc != null) {
						return featureLoc.getVariations().stream()
								.map(v -> new CompletionSuggestion(v.getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
		
	}

	
}
