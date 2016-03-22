package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import gnu.trove.procedure.TIntObjectProcedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAminoAcidResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@CommandClass(
		commandWords={"amino-acid", "frequency"}, 
		description = "Compute amino acid frequencies for a given feature location", 
		docoptUsages = { "[-c] [-w <whereClause>] -r <acRefName> -f <featureName>" },
		docoptOptions = { 
		"-c, --recursive                                Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
		"-f <featureName>, --featureName <featureName>  Feature to translate"
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference.",
				metaTags = {}	
)
public class AlignmentAminoAcidFrequencyCommand extends AlignmentModeCommand<AlignmentAminoAcidFrequencyResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}
	
	@Override
	public AlignmentAminoAcidFrequencyResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause);
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, referenceName);
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(referenceName, featureName), false);
		Feature feature = scannedFeatureLoc.getFeature();
		feature.checkCodesAminoAcids();

		TIntObjectMap<RefCodonInfo> codonToRefCodonInfo = new TIntObjectHashMap<RefCodonInfo>();
		
		int codon1Start = scannedFeatureLoc.getCodon1Start(cmdContext);
		for(FeatureSegment featureSeg: scannedFeatureLoc.getSegments()) {
			for(int i = featureSeg.getRefStart(); i <= featureSeg.getRefEnd(); i += 3) {
				codonToRefCodonInfo.put(TranslationUtils.getCodon(codon1Start, i), new RefCodonInfo());
			}
		}
		
		for(AlignmentMember almtMember: almtMembers) {
			MemberAminoAcidResult memberAminoAcidsResult = 
					MemberAminoAcidCommand.memberAminoAcids(cmdContext, almtMember, 
							ancConstrainingRef, scannedFeatureLoc);
			
			List<Map<String, Object>> memberAaRows = memberAminoAcidsResult.asListOfMaps();
			for(Map<String, Object> memberAaRow: memberAaRows) {
				Integer codon = (Integer) memberAaRow.get(MemberAminoAcidResult.CODON_LABEL);
				char aa = ((String) memberAaRow.get(MemberAminoAcidResult.AMINO_ACID)).charAt(0);
				codonToRefCodonInfo.get(codon).addAaMamber(aa);
			}
		}

		List<Map<String, Object>> resultRowData = new ArrayList<Map<String, Object>>();
		codonToRefCodonInfo.forEachEntry(new TIntObjectProcedure<RefCodonInfo>() {
			@Override
			public boolean execute(int codon, RefCodonInfo refCodonInfo) {
				refCodonInfo.aaToMemberCount.forEachEntry(new TCharIntProcedure() {
					@Override
					public boolean execute(char aa, int numMembers) {
						Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
						resultRow.put(AlignmentAminoAcidFrequencyResult.CODON, codon);
						resultRow.put(AlignmentAminoAcidFrequencyResult.AMINO_ACID, new String(new char[]{aa}));
						resultRow.put(AlignmentAminoAcidFrequencyResult.NUM_MEMBERS, numMembers);
						double pctMembers = 100.0 * numMembers / (double) refCodonInfo.membersAtCodon;
						resultRow.put(AlignmentAminoAcidFrequencyResult.PERCENTAGE_MEMBERS, pctMembers);
						resultRowData.add(resultRow);
						return true;
					}
				});
				return true;
			}
		});
		Collections.sort(resultRowData, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> row1, Map<String, Object> row2) {
				Integer codon1 = (Integer) row1.get(AlignmentAminoAcidFrequencyResult.CODON);
				Integer codon2 = (Integer) row2.get(AlignmentAminoAcidFrequencyResult.CODON);
				return Integer.compare(codon1, codon2);
			}
		});
		return new AlignmentAminoAcidFrequencyResult(resultRowData);
	}

	
	private class RefCodonInfo {
		TCharIntMap aaToMemberCount = new TCharIntHashMap();
		int membersAtCodon = 0;
		public void addAaMamber(char aaChar) {
			aaToMemberCount.adjustOrPutValue(aaChar, 1, 1);
			membersAtCodon++;
		}
	}

	
	@CompleterClass
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {}

	
}
