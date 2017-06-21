package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;

public abstract class BaseSamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	public static final String MIN_Q_SCORE = "minQScore";
	public static final String MIN_DEPTH = "minDepth";
	
	public static final String SAM_REF_SENSE = "samRefSense";
	
	private String fileName;
	private String samRefName;
	
	private Optional<Integer> minQScore;
	private Optional<Integer> minDepth;
	
	private Optional<SamRefSense> samRefSense;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.minQScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_Q_SCORE, 0, true, 99, true, false));
		this.minDepth = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_DEPTH, 0, true, null, false, false));
		this.samRefSense = Optional.ofNullable(PluginUtils.configureEnumProperty(SamRefSense.class, configElem, SAM_REF_SENSE, false));
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

	protected SamRefSense getSamRefSense(SamReporter samReporter) {
		return samRefSense.orElse(samReporter.getDefaultSamRefSense());
	}
	
	protected static class SamRefInfo {
		private int samRefIndex;
		private String samRefName;
		private int samRefLength;

		public SamRefInfo(int samRefIndex, String samRefName, int samRefLength) {
			super();
			this.samRefIndex = samRefIndex;
			this.samRefName = samRefName;
			this.samRefLength = samRefLength;
		}

		public int getSamRefIndex() {
			return samRefIndex;
		}

		public String getSamRefName() {
			return samRefName;
		}

		public int getSamRefLength() {
			return samRefLength;
		}
	}
	
	protected SamRefInfo getSamRefInfo(ConsoleCommandContext consoleCmdContext, SamReporter samReporter) {
		String samRefName;
		int samRefLength;
		int samRefIndex;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
	        SAMSequenceRecord samReference = samReader.getFileHeader().getSequenceDictionary().getSequence(samRefName);
	        samRefLength = samReference.getSequenceLength();
	        samRefIndex = samReference.getSequenceIndex();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new SamRefInfo(samRefIndex, samRefName, samRefLength);
	}

}
