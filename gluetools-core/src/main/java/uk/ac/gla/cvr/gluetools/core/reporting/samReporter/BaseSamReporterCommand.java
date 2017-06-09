package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseSamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	public static final String MIN_Q_SCORE = "minQScore";
	public static final String MIN_DEPTH = "minDepth";
	
	private String fileName;
	private String samRefName;
	
	private Optional<Integer> minQScore;
	private Optional<Integer> minDepth;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.minQScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_Q_SCORE, 0, true, 99, true, false));
		this.minDepth = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_DEPTH, 0, true, null, false, false));
	}

	protected String getFileName() {
		return fileName;
	}

	protected String getSuppliedSamRefName() {
		return samRefName;
	}

	protected int getMinQScore(SamReporter samReporter) {
		return minQScore.orElse(samReporter.getDefaultMinQScore());
	}

	protected int getMinDepth(SamReporter samReporter) {
		return minDepth.orElse(samReporter.getDefaultMinDepth());
	}

}
