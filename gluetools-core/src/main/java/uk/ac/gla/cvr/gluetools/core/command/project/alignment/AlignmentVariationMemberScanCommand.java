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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@CommandClass(
		commandWords={"variation", "member", "scan"}, 
		description = "Scan members for a specific variation", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName> -v <variationName> [-e]" },
		docoptOptions = { 
		"-c, --recursive                                      Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>        Qualify members",
		"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>        Feature name",
		"-v <variationName>, --variationName <variationName>  Variation name",
		"-e, --excludeAbsent                                  Exclude members where absent",
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
	}
	
	@Override
	public AlignmentVariationMemberScanResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, acRefName);
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
		Variation variation = 
				GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(acRefName, featureName, variationName), false);
		List<MemberVariationScanResult> resultRowData = alignmentMemberVariationScan(
				cmdContext, alignment, ancConstrainingRef, scannedFeatureLoc, variation, almtMembers, excludeAbsent);
		return new AlignmentVariationMemberScanResult(resultRowData);
	}

	public static List<MemberVariationScanResult> alignmentMemberVariationScan(
			CommandContext cmdContext, Alignment alignment, 
			ReferenceSequence ancConstrainingRef, FeatureLocation scannedFeatureLoc, Variation variation,
			List<AlignmentMember> almtMembers, boolean excludeAbsent) {

		List<MemberVariationScanResult> membVsrList = new ArrayList<MemberVariationScanResult>();
			
		for(AlignmentMember almtMember: almtMembers) {
			
			List<VariationScanResult> scanResults = 
					MemberVariationScanCommand.memberVariationScan(cmdContext, almtMember, ancConstrainingRef, 
							scannedFeatureLoc, Arrays.asList(variation), excludeAbsent);
			if(scanResults.size() > 0) {
				membVsrList.add(new MemberVariationScanResult(almtMember, scanResults.get(0)));
			}
		}
		return membVsrList;
	}

	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("variationName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
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
