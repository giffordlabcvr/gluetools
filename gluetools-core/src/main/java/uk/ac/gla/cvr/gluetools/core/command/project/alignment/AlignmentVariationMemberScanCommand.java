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
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResultRow;

@CommandClass(
		commandWords={"variation", "member", "scan"}, 
		description = "Scan members for a specific variation", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName> -v <variationName> [-e] [-l [-s [-n] [-o]]]" },
		docoptOptions = { 
		"-c, --recursive                                      Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>        Qualify members",
		"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>        Feature name",
		"-v <variationName>, --variationName <variationName>  Variation name",
		"-e, --excludeAbsent                                  Exclude members where absent",
		"-l, --showPatternLocsSeparately                      Add row per pattern location",
		"-s, --showMatchValuesSeparately                      Add row per match value",
		"-n, --showMatchNtLocations                           Add match NT start/end columns",
		"-o, --showMatchLcLocations                           Add codon start/end columns"
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference.",
				metaTags = {}	
)
public class AlignmentVariationMemberScanCommand extends AlignmentModeCommand<AlignmentVariationMemberScanResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String VARIATION_NAME = "variationName";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";

	private Boolean recursive;
	private Optional<Expression> whereClause;

	private String acRefName;
	private String featureName;
	private String variationName;
	private Boolean excludeAbsent;
	private VariationScanRenderHints variationScanRenderHints = new VariationScanRenderHints();

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
		this.variationScanRenderHints.configure(pluginConfigContext, configElem);
	}
	
	@Override
	public AlignmentVariationMemberScanResult execute(CommandContext cmdContext) {
		List<MemberVariationScanResult> resultRowData = alignmentMemberVariationScan(
				cmdContext, variationScanRenderHints, getAlignmentName(), acRefName, featureName, variationName, whereClause, recursive, excludeAbsent);
		return new AlignmentVariationMemberScanResult(variationScanRenderHints, resultRowData);
	}

	public static List<MemberVariationScanResult> alignmentMemberVariationScan(
			CommandContext cmdContext, VariationScanRenderHints variationScanRenderHints, String alignmentName, 
			String acRefName, String featureName, String variationName,
			Optional<Expression> whereClause, boolean recursive,
			boolean excludeAbsent) {
		
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, alignmentName, recursive, whereClause);
		
		int batchSize = 500;
		int offset = 0;
		
		List<MemberVariationScanResult> membVsrList = new ArrayList<MemberVariationScanResult>();

		while(offset < totalMembers) {
			int lastBatchIndex = Math.min(offset+batchSize, totalMembers);

			Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), false);
			ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
			FeatureLocation scannedFeatureLoc = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
			Variation variation = 
					GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(acRefName, featureName, variationName), false);

			GlueLogger.getGlueLogger().finest("Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause, offset, batchSize, batchSize);
			GlueLogger.getGlueLogger().finest("Scanning variation for members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			for(AlignmentMember almtMember: almtMembers) {
				List<VariationScanResultRow> scanResultRows = 
						variationScanRenderHints.scanResultsToResultRows(MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, ancConstrainingRef, 
								scannedFeatureLoc, Arrays.asList(variation), excludeAbsent));
				for(VariationScanResultRow vsrr : scanResultRows) {
					membVsrList.add(new MemberVariationScanResult(almtMember, vsrr));
				}
			}
			cmdContext.newObjectContext();
			offset = offset+batchSize;
		}
		GlueLogger.getGlueLogger().finest("Scanned variation for "+totalMembers+" members");
		cmdContext.newObjectContext();
		return membVsrList;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("variationName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("acRefName");
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
