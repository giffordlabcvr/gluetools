package uk.ac.gla.cvr.gluetools.core.variationFrequencies;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

/* module which generates varAlmtNote objects relating variations to alignments, 
 * based on the frequency of the variation within the alignment. */
@PluginClass(elemName="variationFrequenciesGenerator")
public class VariationFrequenciesGenerator extends ModulePlugin<VariationFrequenciesGenerator> {

	public static class AlignmentVariationReport {
		Alignment alignment;
		Variation variation;
		double frequency;
	}

	public static String FREQUENCY_FIELD_NAME = "frequencyFieldName";
	public static String MIN_FREQUENCY_PCT = "minFrequencyPct";
	public static String MIN_SAMPLE_SIZE = "minSampleSize";
	
	private String frequencyFieldName; // DOUBLE field on var-almt-note, where the frequency will be set.
	private Double minFrequencyPct; // if frequency percentage is below this value, no varAlmtNote will be generated / updated.
	private Integer minSampleSize; // if the number of members sampled is below this threshold, no varAlmtNote will be generated / updated.
	
	public VariationFrequenciesGenerator() {
		super();
		addModulePluginCmdClass(GenerateVariationFrequenciesCommand.class);
		addSimplePropertyName(FREQUENCY_FIELD_NAME);
		addSimplePropertyName(MIN_FREQUENCY_PCT);
		addSimplePropertyName(MIN_SAMPLE_SIZE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.frequencyFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, FREQUENCY_FIELD_NAME, false)).orElse("frequency");
		this.minFrequencyPct = PluginUtils.configureDoubleProperty(configElem, MIN_FREQUENCY_PCT, false);
		this.minSampleSize = PluginUtils.configureIntProperty(configElem, MIN_SAMPLE_SIZE, false);
	}

	public void generateAlmtVarNotes(
			CommandContext cmdContext, Alignment alignment,
			List<VariationScanMemberCount> scanCounts, List<VariationFrequenciesGenerator.AlignmentVariationReport> almtVarReports) {
		for(VariationScanMemberCount memberCount: scanCounts) {
			double frequency = memberCount.getPctWherePresent();
			if( (minFrequencyPct == null || frequency >= minFrequencyPct)  ) {
				if( (minSampleSize == null || memberCount.getMembersWherePresent() >= minSampleSize)  ) {
					
					// TODO -- generate / update the almt-var note!
					
					
					VariationFrequenciesGenerator.AlignmentVariationReport almtVarReport = new VariationFrequenciesGenerator.AlignmentVariationReport();
					almtVarReport.alignment = alignment;
					almtVarReport.variation = memberCount.getVariation();
					almtVarReport.frequency = frequency;
					almtVarReports.add(almtVarReport);
					if(almtVarReports.size() % 500 == 0) {
						log("Generated "+almtVarReports.size()+" alignment-variation frequencies");
					}
				}
			}
		}
	}
	
}
