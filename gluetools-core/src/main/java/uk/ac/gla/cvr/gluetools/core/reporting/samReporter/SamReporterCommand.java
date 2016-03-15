package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public abstract class SamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String AUTO_ALIGN = "autoAlign";
	
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";
	
	private String fileName;
	private String samRefName;
	private String acRefName;
	private String featureName;
	private boolean autoAlign;
	private String targetRefName;
	private String tipAlmtName;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.autoAlign = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, AUTO_ALIGN, false)).orElse(false);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, false);
		this.tipAlmtName = PluginUtils.configureStringProperty(configElem, TIP_ALMT_NAME, false);
		super.configure(pluginConfigContext, configElem);
	}

	protected List<QueryAlignedSegment> getSamRefToTargetRefSegs(
			CommandContext cmdContext, SamReporter samReporter,
			ConsoleCommandContext consoleCmdContext, ReferenceSequence targetRef) {
		List<QueryAlignedSegment> samRefToTargetRefSegs;
		if(autoAlign) {
			// auto-align consensus to target ref
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			Map<String, DNASequence> samConsensus = SamUtils.getSamConsensus(consoleCmdContext, samRefName, fileName, "samConsensus");
			AlignerResult alignerResult = aligner.doAlign(cmdContext, targetRef.getName(), samConsensus);
			// extract segments from aligner result
			samRefToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get("samConsensus");
		} else {
			// sam ref is same sequence as target ref, so just a single self-mapping segment.
			int targetRefLength = targetRef.getSequence().getSequenceObject().getNucleotides(consoleCmdContext).length();
			samRefToTargetRefSegs = Arrays.asList(new QueryAlignedSegment(1, targetRefLength, 1, targetRefLength));
		}
		return samRefToTargetRefSegs;
	}

	protected String getFileName() {
		return fileName;
	}

	protected String getSuppliedSamRefName() {
		return samRefName;
	}

	protected String getAcRefName() {
		return acRefName;
	}

	protected String getFeatureName() {
		return featureName;
	}

	protected String getTargetRefName(CommandContext cmdContext, SamReporter samReporter, String samRefName) {
		return samReporter.targetRefNameFromSamRefName(cmdContext, samRefName, targetRefName);
	}

	protected String getTipAlmtName() {
		return tipAlmtName;
	}



	
}
