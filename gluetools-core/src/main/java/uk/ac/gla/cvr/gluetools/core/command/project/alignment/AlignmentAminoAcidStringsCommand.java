package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.AminoAcidStringFrequency;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
		commandWords={"amino-acid", "strings"}, 
		description = "Compute the amino acid strings and their frequencies within a given feature location", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName> <lcStart> <lcEnd>" },
		docoptOptions = { 
		"-c, --recursive                                Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"The <lcStart> and <lcEnd> arguments specify labeled codons, the returned set of strings will be within this region, "
		+ "including the endpoints.",
		metaTags = {}	
)
public class AlignmentAminoAcidStringsCommand extends AlignmentModeCommand<AlignmentAminoAcidStringsResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";

	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String acRefName;
	private String featureName;
	private String lcStart;
	private String lcEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, true);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, true);

	}
	@Override
	public AlignmentAminoAcidStringsResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		alignment.getConstrainingRef(); // check constrained
		// check it is a coding feature.
		GlueDataObject
			.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false)
			.getFeature().checkCodesAminoAcids();
		List<AminoAcidStringFrequency> resultRowData = alignmentAminoAcidStrings(
				cmdContext, getAlignmentName(), acRefName, featureName, whereClause, recursive, lcStart, lcEnd);
		return new AlignmentAminoAcidStringsResult(resultRowData);
	}

	public static List<AminoAcidStringFrequency> alignmentAminoAcidStrings(
			CommandContext cmdContext, String almtName, String relRefName, String featureName, 
			Optional<Expression> whereClause, Boolean recursive, 
			String lcStart, String lcEnd) {

		SimpleAlignmentColumnsSelector almtColsSelector = 
					new SimpleAlignmentColumnsSelector(relRefName, featureName, null, null, lcStart, lcEnd);
		
		AAStringInfo aaStringInfo = new AAStringInfo();
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(almtName, recursive, whereClause);

		AbstractAlmtRowConsumer almtRowConsumer = new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				if(alignmentRowString.contains("X")) {
					return;
				}
				aaStringInfo.registerString(alignmentRowString);
			}
		};
		FastaProteinAlignmentExporter.exportAlignment(cmdContext,
				featureName, almtColsSelector, true, queryMemberSupplier, almtRowConsumer);
		
		ArrayList<AminoAcidStringFrequency> aasfList = aaStringInfo.toAASFList();
		
		Collections.sort(aasfList, new Comparator<AminoAcidStringFrequency>() {
			@Override
			public int compare(AminoAcidStringFrequency o1, AminoAcidStringFrequency o2) {
				return Double.compare(o2.getPctMembers(), o1.getPctMembers());
			}
		});
		return aasfList;
	}
	
	private static class AAStringInfo {
		LinkedHashMap<String, Integer> stringToNumMembers = new LinkedHashMap<String, Integer>();
		int totalMembers = 0;
		public void registerString(String aaString) {
			Integer currentNumMembers = stringToNumMembers.computeIfAbsent(aaString, aas -> 0);
			stringToNumMembers.put(aaString, currentNumMembers+1);
			totalMembers++;
		}
		
		public ArrayList<AminoAcidStringFrequency> toAASFList() {
			if(totalMembers == 0) {
				return new ArrayList<AminoAcidStringFrequency>();
			}
			return new ArrayList<AminoAcidStringFrequency>(stringToNumMembers.entrySet().stream()
				.map(e -> {
					String aaString = e.getKey();
					Integer numMembers = e.getValue();
					return new AminoAcidStringFrequency(aaString, numMembers, totalMembers, (numMembers * 100.0)/totalMembers);
				})
				.collect(Collectors.toList()));
		}
	}
	
	
	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
