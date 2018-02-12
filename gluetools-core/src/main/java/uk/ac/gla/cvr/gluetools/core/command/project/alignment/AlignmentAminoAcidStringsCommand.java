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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.AminoAcidStringFrequency;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;


@CommandClass(
		commandWords={"amino-acid", "strings"}, 
		description = "Compute the amino acid strings and their frequencies within a genome region", 
		docoptUsages = { "[-c] [-w <whereClause>] [ -x | -g ] (-a <almtColsSelector> -f <featureName> [-s] | -r <relRefName> -f <featureName> <lcStart> <lcEnd>)" },
		docoptOptions = { 
		"-c, --recursive                                               Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>                 Qualify members",
		"-a <almtColsSelector>, --almtColsSelector <almtColsSelector>  Alignment columns selector module",
		"-f <featureName>, --featureName <featureName>                 Coding feature to translate",
		"-r <relRefName>, --relRefName <relRefName>                    Related reference sequence",
		"-s, --shortForm                                               Elide discontiguities with slash",
		"-x, --excludeAnyGap                                           Exclude if any residue missing",
		"-g, --excludeAllGap                                           Exclude if all residues missing",
		},
		furtherHelp = 
		"The command may be run in two alternative modes. The first possibility is to use an alignment columns selector module. "+
		"This allows discontiguous regions to be selected. In this case the module may only use amino acid region selectors, and may " +
		"only refer to descendent features of the named feature. If the --shortForm option is used, then any unselected regions are "+
		"elided in the output using '/'. "+
		"The second possibility is to specifically identify a single contiguous genome region. In this case, "+
		"if this alignment is constrained, <relRefName> names a reference sequence constraining an ancestor alignment "
		+ "of this alignment. If unconstrained, <relRefName> names a reference sequence which is a member of this alignment. "+
		"The <featureName> arguments names a feature which has a location defined on the named reference. "+
		"The <lcStart> and <lcEnd> arguments specify labeled codons, the returned set of strings will be within this region, "
		+ "including the endpoints.",
		metaTags = {}	
)
public class AlignmentAminoAcidStringsCommand extends AlignmentModeCommand<AlignmentAminoAcidStringsResult> {

	
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALMT_COLS_SELECTOR = "almtColsSelector";
	public static final String FEATURE_NAME = "featureName";
	public static final String SHORT_FORM = "shortForm";
	public static final String REL_REF_NAME = "relRefName";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	public static final String EXCLUDE_ANY_GAP = "excludeAnyGap";
	public static final String EXCLUDE_ALL_GAP = "excludeAllGap";

	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String almtColsSelectorModuleName;
	private String featureName;
	private Boolean shortForm;
	private String relRefName;
	private String lcStart;
	private String lcEnd;
	private Boolean excludeAnyGap;
	private Boolean excludeAllGap;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.almtColsSelectorModuleName = PluginUtils.configureStringProperty(configElem, ALMT_COLS_SELECTOR, false);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.shortForm = PluginUtils.configureBooleanProperty(configElem, SHORT_FORM, true);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		this.shortForm = PluginUtils.configureBooleanProperty(configElem, SHORT_FORM, true);
		this.excludeAnyGap = PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ANY_GAP, true);
		this.excludeAllGap = PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ALL_GAP, true);

		if(this.almtColsSelectorModuleName == null) {
			if(this.shortForm) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --shortForm option may only be used when a columns selector is specified");
			}
			if(this.relRefName == null || this.lcStart == null || this.lcEnd == null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "All the arguments for a specific contiguous genome region must be supplied");
			}
		} else {
			if(this.relRefName != null || this.lcStart != null || this.lcEnd != null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "If a columns selector module is specified, arguments for a specific contiguous genome region may not be used");
			}
		}
	}
	@Override
	public AlignmentAminoAcidStringsResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);

		List<AminoAcidStringFrequency> resultRowData;

		IAlignmentColumnsSelector iAlmtColsSelector;
		
		if(almtColsSelectorModuleName != null) {
			AlignmentColumnsSelector almtColsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, almtColsSelectorModuleName);
			Feature parentFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
			parentFeature.checkCodesAminoAcids();
			almtColsSelector.checkWithinCodingParentFeature(cmdContext, parentFeature);
			String selectorRelRefName = almtColsSelector.getRelatedRefName();
			// check feature location exists on selectorRelRef
			GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(selectorRelRefName, featureName));
			iAlmtColsSelector = almtColsSelector;
		} else {
			alignment.getRelatedRef(cmdContext, relRefName); // check related Ref.
			// check it is a coding feature.
			GlueDataObject
			.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false)
			.getFeature().checkCodesAminoAcids();
			iAlmtColsSelector = 
					new SimpleAlignmentColumnsSelector(relRefName, featureName, null, null, lcStart, lcEnd);
		}
		resultRowData = alignmentAminoAcidStrings(cmdContext, getAlignmentName(), whereClause,
				recursive, featureName, shortForm, 
				excludeAnyGap, excludeAllGap, iAlmtColsSelector);
		return new AlignmentAminoAcidStringsResult(resultRowData);
	}

	private static List<AminoAcidStringFrequency> alignmentAminoAcidStrings(
			CommandContext cmdContext, String almtName,
			Optional<Expression> whereClause, Boolean recursive,
			String featureName, Boolean shortForm, Boolean excludeAnyGap, Boolean excludeAllGap, 
			IAlignmentColumnsSelector almtColsSelector) {
		AAStringInfo aaStringInfo = new AAStringInfo();
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(almtName, recursive, whereClause);

		final List<ReferenceSegment> refSegs;
		if(shortForm) {
			refSegs = almtColsSelector.selectAlignmentColumns(cmdContext);
		} else {
			refSegs = null;
		}
		
		AbstractAlmtRowConsumer almtRowConsumer = new AbstractAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				alignmentRowString = alignmentRowString.replace('X', '-');
				if(shortForm) {
					StringBuffer elidedStringBuffer = new StringBuffer();
					int minRefStart = ReferenceSegment.minRefStart(refSegs);
					for(int i = 0; i < refSegs.size(); i++) {
						if(i > 0) {
							elidedStringBuffer.append("/");
						}
						int aaCharStart = (refSegs.get(i).getRefStart() - minRefStart) / 3;
						int aaCharEnd = aaCharStart + refSegs.get(i).getCurrentLength() / 3;
						elidedStringBuffer.append(alignmentRowString.substring(aaCharStart, aaCharEnd));
					}
					alignmentRowString = elidedStringBuffer.toString();
				}
				if(excludeAnyGap && alignmentRowString.contains("-")) {
					return;
				}
				if(excludeAllGap && alignmentRowString.matches("^[-/]*$")) {
					return;
				}
				aaStringInfo.registerString(alignmentRowString);
			}
		};
		FastaProteinAlignmentExporter.exportAlignment(cmdContext,
				featureName, almtColsSelector, false, queryMemberSupplier, almtRowConsumer);
		
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
	public static final class Completer extends FeatureOfAncConstrainingRefCompleter {
		public Completer() {
			super();
			registerModuleNameLookup("almtColsSelector", "alignmentColumnsSelector");
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideAlignmentMode insideAlignmentMode = (InsideAlignmentMode) cmdContext.peekCommandMode();
					String almtName = insideAlignmentMode.getAlignmentName();
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					return alignment.getAncConstrainingRefs().stream()
							.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new SimpleDataObjectNameInstantiator(Feature.class, Feature.NAME_PROPERTY) {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("relRefName");
					if(referenceName == null) {
						return super.instantiate(cmdContext, cmdClass, bindings, prefix);
					}
					ReferenceSequence referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					if(referenceSequence != null) {
						return referenceSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
		
	}

	
}
