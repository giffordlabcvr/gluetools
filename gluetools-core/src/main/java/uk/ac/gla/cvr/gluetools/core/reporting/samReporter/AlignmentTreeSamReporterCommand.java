package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// SAM reporter command which links the SAM reads to the GLUE alignment tree before performing
// some kind of analysis.
public abstract class AlignmentTreeSamReporterCommand<R extends CommandResult> extends BaseSamReporterCommand<R> {

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";

	public static final String MAX_LIKELIHOOD_PLACER = "maxLikelihoodPlacer";
	
	public static final String AUTO_ALIGN = "autoAlign";
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";
	
	private String acRefName;
	private String featureName;

	private boolean maxLikelihoodPlacer;

	private boolean autoAlign;
	private String targetRefName;
	private String tipAlmtName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.maxLikelihoodPlacer = PluginUtils.configureBooleanProperty(configElem, MAX_LIKELIHOOD_PLACER, false);
		this.autoAlign = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, AUTO_ALIGN, false)).orElse(false);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, false);
		this.tipAlmtName = PluginUtils.configureStringProperty(configElem, TIP_ALMT_NAME, false);
	}

	
	
	protected List<QueryAlignedSegment> getSamRefToTargetRefSegs(
			CommandContext cmdContext, SamReporter samReporter,
			ConsoleCommandContext consoleCmdContext, ReferenceSequence targetRef, DNASequence consensusSequence) {
		List<QueryAlignedSegment> samRefToTargetRefSegs;
		if(autoAlign || maxLikelihoodPlacer) {
			// auto-align consensus to target ref
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			Map<String, DNASequence> samConsensus;
			if(consensusSequence == null) {
				// compute consensus if we don't already have it.
				samConsensus = 
						SamUtils.getSamConsensus(consoleCmdContext, getFileName(), samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", 
								getMinQScore(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
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

	protected String getAcRefName() {
		return acRefName;
	}

	protected String getFeatureName() {
		return featureName;
	}

	protected boolean useMaxLikelihoodPlacer() {
		return maxLikelihoodPlacer;
	}

	protected String establishTargetRefName(CommandContext cmdContext, SamReporter samReporter, String samRefName, DNASequence consensusSequence) {
		return samReporter.establishTargetRefName(cmdContext, samRefName, targetRefName);
	}

	protected String getTipAlmtName(CommandContext cmdContext, SamReporter samReporter, String samRefName) {
		return samReporter.tipAlignmentNameFromSamRefName(cmdContext, samRefName, tipAlmtName);
	}



	
}
