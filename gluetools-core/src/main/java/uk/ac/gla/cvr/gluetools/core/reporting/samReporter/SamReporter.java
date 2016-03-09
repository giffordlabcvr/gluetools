package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@PluginClass(elemName="samReporter")
public class SamReporter extends ModulePlugin<SamReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String READ_LOG_INTERVAL = "readLogInterval";
	public static final String DEFAULT_REFERENCE_NAME = "defaultReferenceName";
	public static final String TIP_ALIGNMENT_MEMBER_EXTRACTOR_FORMATTER = "tipAlignmentMemberExtractorFormatter";

	private String alignerModuleName;
	private Integer readLogInterval;
	private String defaultReferenceName;
	// optional -- transforms SAM reference name to a where clause identifying the tip alignment member.
	private RegexExtractorFormatter tipAlignmentMemberExtractorFormatter;
	
	public SamReporter() {
		super();
		addModulePluginCmdClass(SamVariationScanCommand.class);
		addModulePluginCmdClass(SamNucleotideCommand.class);
		addModulePluginCmdClass(SamAminoAcidCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);
		addSimplePropertyName(READ_LOG_INTERVAL);
		addSimplePropertyName(DEFAULT_REFERENCE_NAME);
		
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
		this.readLogInterval = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, READ_LOG_INTERVAL, false)).orElse(20000);
		this.defaultReferenceName = PluginUtils.configureStringProperty(configElem, DEFAULT_REFERENCE_NAME, false);
		Element tipAlignmentMemberExtractorFormatterElem = PluginUtils.findConfigElement(configElem, TIP_ALIGNMENT_MEMBER_EXTRACTOR_FORMATTER);
		if(tipAlignmentMemberExtractorFormatterElem != null) {
			tipAlignmentMemberExtractorFormatter = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, tipAlignmentMemberExtractorFormatterElem);
		} 
	}

	public String getAlignerModuleName() {
		return alignerModuleName;
	}

	public String getDefaultReferenceName() {
		return defaultReferenceName;
	}

	public String extractTipAlignmentMemberWhereClause(String samRefName) {
		if(tipAlignmentMemberExtractorFormatter != null) {
			String expressionString = tipAlignmentMemberExtractorFormatter.matchAndConvert(samRefName);
			return expressionString;
		}
		return null;
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
