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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

// SAM reporter command which links the SAM reads to a GLUE reference before performing
// some kind of analysis.
public abstract class ReferenceLinkedSamReporterCommand<R extends CommandResult> extends BaseSamReporterCommand<R> {

	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";

	public static final String MAX_LIKELIHOOD_PLACER = "maxLikelihoodPlacer";
	
	public static final String AUTO_ALIGN = "autoAlign";
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String LINKING_ALMT_NAME = "linkingAlmtName";
	
	private String relRefName;
	private String featureName;

	private boolean maxLikelihoodPlacer;

	private boolean autoAlign;
	private String targetRefName;
	private String linkingAlmtName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
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
	}

	
	
	protected List<QueryAlignedSegment> getSamRefToTargetRefSegs(
			CommandContext cmdContext, SamReporter samReporter, SamFileSession samFileSession, 
			ConsoleCommandContext consoleCmdContext, ReferenceSequence targetRef, DNASequence consensusSequence) {
		List<QueryAlignedSegment> samRefToTargetRefSegs;
		if(autoAlign || maxLikelihoodPlacer) {
			// auto-align consensus to target ref
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			Map<String, DNASequence> samConsensus;
			if(consensusSequence == null) {
				// compute consensus if we don't already have it.
				samConsensus = 
						SamUtils.getSamConsensus(consoleCmdContext, getFileName(), samFileSession, samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", 
								getMinQScore(samReporter), getMinMapQ(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
			} else {
				samConsensus = new LinkedHashMap<String, DNASequence>();
				samConsensus.put("samConsensus", consensusSequence);
			}
			AlignerResult alignerResult = aligner.computeConstrained(cmdContext, targetRef.getName(), samConsensus);
			// extract segments from aligner result
			samRefToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get("samConsensus");
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
	
	
}
