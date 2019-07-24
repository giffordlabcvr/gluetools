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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import gnu.trove.procedure.TIntObjectProcedure;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractLqaaAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
		commandWords={"amino-acid", "frequency"}, 
		description = "Compute amino acid frequencies for a range of codon positions within a coding feature", 
		docoptUsages = { "[-c] [-w <whereClause>] (-a <almtColsSelector> | -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd>])" },
		docoptOptions = { 
		"-c, --recursive                                               Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>                 Qualify members",
		"-a <almtColsSelector>, --almtColsSelector <almtColsSelector>  Alignment columns selector module",
		"-r <relRefName>, --relRefName <relRefName>                    Related reference",
		"-f <featureName>, --featureName <featureName>                 Feature to translate",
		"-l, --labelledCodon                                           Region between codon labels",
		},
		furtherHelp = 
		"The command may be run in two alternative modes. The first possibility is to use an alignment columns selector module "+
		"to specify the codon positions. "+
		"This allows discontiguous regions to be selected. In this case the selector module may only use amino acid region selector elements. "+
		"The second possibility is to calculate frequencies for all positions or a contiguous range of positions within a feature. In this case "+
		"the <relRefName> argument names the reference sequence on which the feature is defined."+
		"The <featureName> arguments names the coding feature and the --labeledCodon <lcStart> <lcEnd> specifies a contiguous region. "+
		"If this alignment is constrained, the related reference must constrain an ancestor "+
		"alignment of this alignment. If unconstrained, it may be any reference which is a member of this alignment. ",
				metaTags = {}	
)
public class AlignmentAminoAcidFrequencyCommand extends AlignmentModeCommand<AlignmentAminoAcidFrequencyResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LABELLED_CODON = "labelledCodon";
	public static final String ALMT_COLS_SELECTOR = "almtColsSelector";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";


	// could possibly use FastaAlignmentExportCommandDelegate here.
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String almtColsSelectorModuleName;
	private String relRefName;
	private String featureName;
	private boolean labelledCodon;
	private String lcStart;
	private String lcEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtColsSelectorModuleName = PluginUtils.configureStringProperty(configElem, ALMT_COLS_SELECTOR, false);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.labelledCodon = PluginUtils.configureBooleanProperty(configElem, LABELLED_CODON, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		if(this.almtColsSelectorModuleName == null) {
			if(this.relRefName == null || this.featureName == null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either a columns selector module or a feature location must be specified");
			}
		} else {
			if(this.relRefName != null || this.featureName != null || this.labelledCodon || this.lcStart != null || this.lcEnd != null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "If a columns selector module is specified, arguments for a specific contiguous genome region may not be used");
			}
		}
		if(labelledCodon && (lcStart == null || lcEnd == null)) {
			usageErrorLC();
		}
	}
	
	private void usageErrorLC() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --labelledCodon is used, both <lcStart> and <lcEnd> must be specified");
	}
	
	@Override
	public AlignmentAminoAcidFrequencyResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		IAminoAcidAlignmentColumnsSelector aaAlmtColsSelector = null;
		if(almtColsSelectorModuleName == null) {
			alignment.getRelatedRef(cmdContext, relRefName); // check related ref.
			// check it is a coding feature.
			GlueDataObject
				.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false)
				.getFeature().checkCodesAminoAcids();
			aaAlmtColsSelector = new SimpleAminoAcidColumnsSelector(relRefName, featureName, lcStart, lcEnd);	
		} else {
			aaAlmtColsSelector = Module.resolveModulePlugin(cmdContext, IAminoAcidAlignmentColumnsSelector.class, almtColsSelectorModuleName);
		}
		List<LabeledAminoAcidFrequency> resultRowData = alignmentAminoAcidFrequencies(
				cmdContext, getAlignmentName(), whereClause, recursive, aaAlmtColsSelector);
		return new AlignmentAminoAcidFrequencyResult(resultRowData);
	}

	public static List<LabeledAminoAcidFrequency> alignmentAminoAcidFrequencies(
			CommandContext cmdContext, String almtName,
			Optional<Expression> whereClause, Boolean recursive, 
			IAminoAcidAlignmentColumnsSelector aaAlmtColumnsSelector) {

		
		TIntObjectMap<RefCodonInfo> transcriptionIndexToRefCodonInfo = 
				initTranscriptionIndexToRefInfoMap(cmdContext, aaAlmtColumnsSelector);
		
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(almtName, recursive, whereClause);

		AbstractLqaaAlmtRowConsumer lqaaAlmtRowConsumer = new AbstractLqaaAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext,
					AlignmentMember almtMember,
					List<LabeledQueryAminoAcid> lqaas) {
				for(LabeledQueryAminoAcid lqaa: lqaas) {
					int transcriptionIndex = lqaa.getLabeledAminoAcid().getLabeledCodon().getTranscriptionIndex();
					char singleCharTranslation = lqaa.getLabeledAminoAcid().getTranslationInfo().getSingleCharTranslation();
					if(singleCharTranslation != 'X') {
						transcriptionIndexToRefCodonInfo.get(transcriptionIndex).addAaMamber(singleCharTranslation);
					}
				}
			}
		};
		aaAlmtColumnsSelector.generateLqaaAlignmentRows(cmdContext, true, queryMemberSupplier, lqaaAlmtRowConsumer);
		
		return formLabeledAminoAcidFrequencies(transcriptionIndexToRefCodonInfo);
	}

	private static List<LabeledAminoAcidFrequency> formLabeledAminoAcidFrequencies(
			TIntObjectMap<RefCodonInfo> refNtToRefCodonInfo) {
		List<LabeledAminoAcidFrequency> resultRowData = new ArrayList<LabeledAminoAcidFrequency>();
		refNtToRefCodonInfo.forEachEntry(new TIntObjectProcedure<RefCodonInfo>() {
			@Override
			public boolean execute(int refNt, RefCodonInfo refCodonInfo) {
				refCodonInfo.aaToMemberCount.forEachEntry(new TCharIntProcedure() {
					@Override
					public boolean execute(char aa, int numMembers) {
						double pctMembers = 100.0 * numMembers / (double) refCodonInfo.membersAtCodon;
						String aminoAcid = new String(new char[]{aa});
						LabeledAminoAcidFrequency labeledAminoAcidFrequency = 
								new LabeledAminoAcidFrequency(refCodonInfo.labeledCodon, aminoAcid, numMembers, refCodonInfo.membersAtCodon, pctMembers);
						resultRowData.add(labeledAminoAcidFrequency);
						return true;
					}
				});
				return true;
			}
		});
		resultRowData.sort(new Comparator<LabeledAminoAcidFrequency>() {
			@Override
			public int compare(LabeledAminoAcidFrequency o1, LabeledAminoAcidFrequency o2) {
				int comp = Integer.compare(o1.getLabeledCodon().getTranscriptionIndex(), o2.getLabeledCodon().getTranscriptionIndex());
				if(comp != 0) { return comp; }
				comp = Double.compare(o1.getPctMembers(), o1.getPctMembers());
				if(comp != 0) { return comp; }
				return o1.getAminoAcid().compareTo(o2.getAminoAcid());
			}
			
		});
		
		return resultRowData;
	}

	private static TIntObjectMap<RefCodonInfo> initTranscriptionIndexToRefInfoMap(
			CommandContext cmdContext, IAminoAcidAlignmentColumnsSelector aaAlmtColsSelector) {
		TIntObjectMap<RefCodonInfo> transcriptionIndexToRefCodonInfo = new TIntObjectHashMap<RefCodonInfo>();
		List<LabeledCodon> labeledCodons = aaAlmtColsSelector.selectLabeledCodons(cmdContext);
		for(LabeledCodon labeledCodon: labeledCodons) {
			RefCodonInfo refCodonInfo = new RefCodonInfo(labeledCodon);
			transcriptionIndexToRefCodonInfo.put(labeledCodon.getTranscriptionIndex(), refCodonInfo);
		}
		return transcriptionIndexToRefCodonInfo;
	}


	
	private static class RefCodonInfo {
		LabeledCodon labeledCodon;
		TCharIntMap aaToMemberCount = new TCharIntHashMap();
		int membersAtCodon = 0;

		public RefCodonInfo(LabeledCodon labeledCodon) {
			super();
			this.labeledCodon = labeledCodon;
		}

		public void addAaMamber(char aaChar) {
			aaToMemberCount.adjustOrPutValue(aaChar, 1, 1);
			membersAtCodon++;
		}
	}

	
	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {
		public Completer() {
			super();
			registerModuleNameLookup("almtColsSelector", "alignmentColumnsSelector");
		}
	}

	
}
