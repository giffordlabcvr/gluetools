package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;

@PluginClass(elemName="samReporter")
public class SamReporter extends ModulePlugin<SamReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String READ_LOG_INTERVAL = "readLogInterval";
	public static final String SAM_REF_TEXT_TO_REFERENCE_QUERY_MODULE_NAME = "samRefTextToReferenceQueryModuleName";
	public static final String SAM_REF_TEXT_TO_TIP_ALMT_QUERY_MODULE_NAME = "samRefTextToTipAlignmentQueryModuleName";
	public static final String SAM_READER_VALIDATION_STRINGENCY = "samReaderValidationStringency";

	private String alignerModuleName;
	// optional -- Module of type textToQueryTransformer.
	// Transforms SAM reference name to a where clause identifying the target reference.
	private String samRefTextToReferenceQueryModuleName;
	// optional -- Module of type textToQueryTransformer.
	// Transforms SAM reference name to a where clause identifying the tip alignment.
	private String samRefTextToTipAlmtQueryModuleName;
	private Integer readLogInterval;
	// STRICT (default), LENIENT, or SILENT
	private ValidationStringency samReaderValidationStringency;
	
	public SamReporter() {
		super();
		addModulePluginCmdClass(SamVariationScanCommand.class);
		addModulePluginCmdClass(SamNucleotideCommand.class);
		addModulePluginCmdClass(SamAminoAcidCommand.class);
		addModulePluginCmdClass(SamNucleotideConsensusCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);
		addSimplePropertyName(READ_LOG_INTERVAL);
		addSimplePropertyName(SAM_REF_TEXT_TO_REFERENCE_QUERY_MODULE_NAME);
		addSimplePropertyName(SAM_REF_TEXT_TO_TIP_ALMT_QUERY_MODULE_NAME);
		addSimplePropertyName(SAM_READER_VALIDATION_STRINGENCY);
		
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
		this.readLogInterval = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, READ_LOG_INTERVAL, false)).orElse(20000);
		this.samRefTextToReferenceQueryModuleName = PluginUtils.configureStringProperty(configElem, SAM_REF_TEXT_TO_REFERENCE_QUERY_MODULE_NAME, false);
		this.samRefTextToTipAlmtQueryModuleName = PluginUtils.configureStringProperty(configElem, SAM_REF_TEXT_TO_TIP_ALMT_QUERY_MODULE_NAME, false);
		this.samReaderValidationStringency = PluginUtils.configureEnumProperty(ValidationStringency.class, configElem, SAM_READER_VALIDATION_STRINGENCY, null);
	}

	public String getAlignerModuleName() {
		return alignerModuleName;
	}

	public ValidationStringency getSamReaderValidationStringency() {
		return samReaderValidationStringency;
	}

	public String targetRefNameFromSamRefName(CommandContext cmdContext, String samRefName, String definedTargetRefName) {
		if(definedTargetRefName != null) {
			return definedTargetRefName;
		}
		if(samRefTextToReferenceQueryModuleName == null) {
			throw new SamReporterCommandException(Code.NO_TARGET_REFERENCE_DEFINED);
		}
		TextToQueryTransformer samRefTextToReferenceQueryTransformer = 
				TextToQueryTransformer.lookupTextToQueryTransformer(cmdContext, samRefTextToReferenceQueryModuleName,
						TextToQueryTransformer.DataClassEnum.ReferenceSequence);
		List<String> referenceSeqNames = samRefTextToReferenceQueryTransformer.textToQuery(cmdContext, samRefName).
				getColumnValues(ReferenceSequence.NAME_PROPERTY);
		if(referenceSeqNames.size() == 0) {
			throw new SamReporterCommandException(Code.TARGET_REFERENCE_NOT_FOUND, samRefName);
		}
		if(referenceSeqNames.size() > 1) {
			throw new SamReporterCommandException(Code.TARGET_REFERENCE_AMBIGUOUS, samRefName, referenceSeqNames.toString());
		}
		return referenceSeqNames.get(0);
	}

	public String tipAlignmentNameFromSamRefName(CommandContext cmdContext, String samRefName, String definedTipAlignmentName) {
		if(definedTipAlignmentName != null) {
			return definedTipAlignmentName;
		}
		if(samRefTextToTipAlmtQueryModuleName == null) {
			throw new SamReporterCommandException(Code.NO_TIP_ALIGNMENT_DEFINED);
		}
		TextToQueryTransformer samRefTextToTipAlmtQueryTransformer = 
				TextToQueryTransformer.lookupTextToQueryTransformer(cmdContext, samRefTextToTipAlmtQueryModuleName,
						TextToQueryTransformer.DataClassEnum.Alignment);
		List<String> tipAlmtNames = samRefTextToTipAlmtQueryTransformer.textToQuery(cmdContext, samRefName).
				getColumnValues(ReferenceSequence.NAME_PROPERTY);
		if(tipAlmtNames.size() == 0) {
			throw new SamReporterCommandException(Code.TIP_ALIGNMENT_NOT_FOUND, samRefName);
		}
		if(tipAlmtNames.size() > 1) {
			throw new SamReporterCommandException(Code.TIP_ALIGNMENT_AMBIGUOUS, samRefName, tipAlmtNames.toString());
		}
		return tipAlmtNames.get(0);
	}
	
	
	
	
	
	public class RecordsCounter {
		int numRecords = 0;
		public void processedRecord() {
			numRecords++;
		}
		public void logRecordsProcessed() {
			if(numRecords % readLogInterval == 0) {
				log(Level.FINE, "Processed "+numRecords+" reads");
			}
		}
		public void logTotalRecordsProcessed() {
			log(Level.FINE, "Total reads processed: "+numRecords);
		}
	}

	public List<QueryAlignedSegment> getReadToSamRefSegs(SAMRecord samRecord) {
		List<QueryAlignedSegment> readToSamRefSegs = new ArrayList<QueryAlignedSegment>();
		samRecord.getAlignmentBlocks().forEach(almtBlock -> {
			int samRefStart = almtBlock.getReferenceStart();
			int samRefEnd = samRefStart + almtBlock.getLength()-1;
			int readStart = almtBlock.getReadStart();
			int readEnd = readStart + almtBlock.getLength()-1;
			readToSamRefSegs.add(new QueryAlignedSegment(samRefStart, samRefEnd, readStart, readEnd));
		});
		return readToSamRefSegs;
	}



	
	
}
