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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleNucleotideColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// SAM reporter command which links the SAM reads to a GLUE reference before performing
// some kind of analysis.
public abstract class ReferenceLinkedSamReporterCommand<R extends CommandResult> extends BaseSamReporterCommand<R> {

	public static final String SELECTOR_NAME = "selectorName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LABELLED_CODON = "labelledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	public static final String NT_REGION = "ntRegion";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";


	public static final String MAX_LIKELIHOOD_PLACER = "maxLikelihoodPlacer";
	
	public static final String AUTO_ALIGN = "autoAlign";
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String LINKING_ALMT_NAME = "linkingAlmtName";
	
	private String relRefName;
	private String featureName;

	
	private String selectorName;
	private Boolean labelledCodon;
	private String lcStart;
	private String lcEnd;
	private Boolean ntRegion;
	private Integer ntStart;
	private Integer ntEnd;
	
	private boolean maxLikelihoodPlacer;

	private boolean autoAlign;
	private String targetRefName;
	private String linkingAlmtName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.selectorName = PluginUtils.configureStringProperty(configElem, SELECTOR_NAME, false);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		this.labelledCodon = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LABELLED_CODON, false)).orElse(false);
		this.lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		this.ntRegion = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, NT_REGION, false)).orElse(false);
		this.ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		this.ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		this.maxLikelihoodPlacer = PluginUtils.configureBooleanProperty(configElem, MAX_LIKELIHOOD_PLACER, false);
		this.autoAlign = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, AUTO_ALIGN, false)).orElse(false);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, false);
		this.linkingAlmtName = PluginUtils.configureStringProperty(configElem, LINKING_ALMT_NAME, true);
		
		if(targetRefName == null && !maxLikelihoodPlacer) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either --maxLikelihoodPlacer or <targetRefName> must be specified");
		}
		if(targetRefName != null && maxLikelihoodPlacer) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Cannot specify both --maxLikelihoodPlacer and <targetRefName>");
		}
		if(selectorName != null && ( relRefName != null || featureName != null )) {
			usageError1a();
		}
		if(selectorName == null && ( relRefName == null || featureName == null ) ) {
			usageError1b();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
		if(selectorName != null && ( ntRegion || labelledCodon )) {
			usageError3a();
		}
		if(labelledCodon && (lcStart == null || lcEnd == null)) {
			usageError4();
		}
		if(ntRegion && labelledCodon) {
			usageError5();
		}
		if(ntRegion && (ntStart == null || ntEnd == null)) {
			usageError6();
		}
		
	}

	private void usageError1a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then <relRefName> and <featureName> may not be used");
	}

	private void usageError1b() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <selectorName> or both <relRefName> and <featureName> must be specified");
	}

	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}

	private void usageError3a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then neither --ntRegion or --labelledCodon may be specified");
	}

	private void usageError4() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --labelledCodon is used, both <lcStart> and <lcEnd> must be specified");
	}
 	private void usageError5() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either --ntRegion or --labelledCodon may be specified, but not both");
	}
 	private void usageError6() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If --ntRegion is used, both <ntStart> and <ntEnd> must be specified");
	}

	
	
	protected List<QueryAlignedSegment> getSamRefToTargetRefSegs(
			CommandContext cmdContext, SamReporter samReporter, SamReporterPreprocessorSession samReporterPreprocessorSession, 
			ConsoleCommandContext consoleCmdContext, ReferenceSequence targetRef) {
		List<QueryAlignedSegment> samRefToTargetRefSegs;
		if(autoAlign || maxLikelihoodPlacer) {
			samRefToTargetRefSegs = samReporterPreprocessorSession.getSamRefToTargetRefSegs(
					consoleCmdContext, samReporter, this, targetRef.getName());
		} else {
			SamRefSense samRefSense = getSamRefSense(samReporter);
			if(!samRefSense.equals(SamRefSense.FORWARD)) {
				throw new SamReporterCommandException(SamReporterCommandException.Code.ILLEGAL_SAM_REF_SENSE, samRefSense.name(), "The <samRefSense> option must be FORWARD unless --autoAlign or --maxLikelihoodPlacer are used");
			}
			
			// sam ref is same sequence as target ref, so just a single self-mapping segment.
			int targetRefLength = targetRef.getSequence().getSequenceObject().getNucleotides(consoleCmdContext).length();
			samRefToTargetRefSegs = Arrays.asList(new QueryAlignedSegment(1, targetRefLength, 1, targetRefLength));
		}
		return samRefToTargetRefSegs;
	}

	protected String getRelatedRefName() {
		return relRefName;
	}

	protected String getFeatureName() {
		return featureName;
	}

	protected boolean useMaxLikelihoodPlacer() {
		return maxLikelihoodPlacer;
	}

	protected String getTargetRefName() {
		return targetRefName;
	}

	protected String getLinkingAlmtName() {
		return linkingAlmtName;
	}
	
	public static class Completer extends BaseSamReporterCommand.Completer {
		public Completer() {
			super();
			registerModuleNameLookup("selectorName", "alignmentColumnsSelector");
			registerDataObjectNameLookup("relRefName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String relRefName = (String) bindings.get("relRefName");
					if(relRefName != null) {
						ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), true);
						if(relatedRef != null) {
							return relatedRef.getFeatureLocations().stream()
									.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
									.collect(Collectors.toList());
						}
					}
					return null;
				}
			});
			registerDataObjectNameLookup("targetRefName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("linkingAlmtName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String targetRefName = (String) bindings.get("targetRefName");
					if(targetRefName != null) {
						ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName), true);
						if(targetRef != null) {
							return targetRef.getSequence().getAlignmentMemberships().stream()
									.map(am -> am.getAlignment())
									.map(a -> new CompletionSuggestion(a.getName(), true))
									.collect(Collectors.toList());
						}
					} else {
						List<Alignment> almts = GlueDataObject
								.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class));
						return almts.stream()
								.map(a -> new CompletionSuggestion(a.getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
	}
	
	public IAlignmentColumnsSelector getNucleotideAlignmentColumnsSelector(CommandContext cmdContext) {
		if(selectorName != null) {
			return Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, selectorName);
		} else if(relRefName != null && featureName != null && ntStart != null && ntEnd != null) {
			return new SimpleNucleotideColumnsSelector(relRefName, featureName, ntStart, ntEnd);
		} else if(relRefName != null && featureName != null && lcStart != null && lcEnd != null) {
			FeatureLocation featureLocation = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));
			int refStart = featureLocation.getLabeledCodon(cmdContext, lcStart).getNtStart();
			int refEnd = featureLocation.getLabeledCodon(cmdContext, lcEnd).getNtEnd();
			return new SimpleNucleotideColumnsSelector(relRefName, featureName, refStart, refEnd);
		} else if(relRefName != null && featureName != null) {
			return new SimpleNucleotideColumnsSelector(relRefName, featureName, null, null);
		} else {
			return null;
		}
	}

	public IAminoAcidAlignmentColumnsSelector getAminoAcidAlignmentColumnsSelector(CommandContext cmdContext) {
		if(selectorName != null) {
			return Module.resolveModulePlugin(cmdContext, IAminoAcidAlignmentColumnsSelector.class, selectorName);
		} else if(relRefName != null && featureName != null) {
			return new SimpleAminoAcidColumnsSelector(relRefName, featureName, lcStart, lcEnd);
		} else {
			return null;
		}
	}

	protected String getSelectorName() {
		return selectorName;
	}

	protected String getLcStart() {
		return lcStart;
	}

	protected String getLcEnd() {
		return lcEnd;
	}

	protected Boolean getLabelledCodon() {
		return labelledCodon;
	}

	protected Boolean getNtRegion() {
		return ntRegion;
	}
	
}
