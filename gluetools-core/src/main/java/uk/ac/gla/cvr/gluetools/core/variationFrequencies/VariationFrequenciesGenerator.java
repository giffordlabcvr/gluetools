package uk.ac.gla.cvr.gluetools.core.variationFrequencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationCreateAlmtNoteCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

@PluginClass(elemName="variationFrequenciesGenerator",
	description="Generates varAlmtNote objects based on the frequency of the variation within the alignment")
public class VariationFrequenciesGenerator extends ModulePlugin<VariationFrequenciesGenerator> {

	public static class AlignmentVariationReport {
		Alignment alignment;
		Variation variation;
		double frequency;
		int totalPresent;
		int totalAbsent;
	}

	public static String FREQUENCY_FIELD_NAME = "frequencyFieldName";
	public static String TOTAL_PRESENT_FIELD_NAME = "totalPresentFieldName";
	public static String TOTAL_ABSENT_FIELD_NAME = "totalAbsentFieldName";
	public static String MIN_FREQUENCY_PCT = "minFrequencyPct";
	public static String MIN_SAMPLE_SIZE = "minSampleSize";
	
	// name of a DOUBLE field on var-almt-note, where the frequency will be set.
	private String frequencyFieldName; 
	// name of an INTEGER field on var-almt-note, where the total present will be set.
	private String totalPresentFieldName; 
	// name of an INTEGER field on var-almt-note, where the total absent will be set.
	private String totalAbsentFieldName; 
	// if frequency percentage is below this value, no varAlmtNote will be generated / updated.
	// default: 1.0%
	private Double minFrequencyPct; 
	// if the number of members sampled is below this threshold, no varAlmtNote will be generated / updated.
	// default: 30
	private Integer minSampleSize; 	
	public VariationFrequenciesGenerator() {
		super();
		registerModulePluginCmdClass(GenerateVariationFrequenciesCommand.class);
		addSimplePropertyName(FREQUENCY_FIELD_NAME);
		addSimplePropertyName(TOTAL_PRESENT_FIELD_NAME);
		addSimplePropertyName(TOTAL_ABSENT_FIELD_NAME);
		addSimplePropertyName(MIN_FREQUENCY_PCT);
		addSimplePropertyName(MIN_SAMPLE_SIZE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.frequencyFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, FREQUENCY_FIELD_NAME, false)).orElse("frequency");
		this.totalPresentFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, TOTAL_PRESENT_FIELD_NAME, false)).orElse("total_present");
		this.totalAbsentFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, TOTAL_ABSENT_FIELD_NAME, false)).orElse("total_absent");
		this.minFrequencyPct = 
				Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_FREQUENCY_PCT, 0.0, true, 100.0, true, false)).orElse(1.0);
		this.minSampleSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_SAMPLE_SIZE, 1, true, null, true, false)).orElse(30);
	}

	public List<AlignmentVariationReport> previewAlmtVarNotes(
			CommandContext cmdContext, Alignment alignment, List<VariationScanMemberCount> scanCounts) {
		List<AlignmentVariationReport> almtVarReports = new ArrayList<AlignmentVariationReport>();
		for(VariationScanMemberCount memberCount: scanCounts) {
			double frequency = memberCount.getPctWherePresent();
			if( (minFrequencyPct == null || frequency >= minFrequencyPct)  ) {
				if( (minSampleSize == null || (memberCount.getMembersWherePresent() + memberCount.getMembersWhereAbsent()) >= minSampleSize)  ) {
					
					VariationFrequenciesGenerator.AlignmentVariationReport almtVarReport = 
							new VariationFrequenciesGenerator.AlignmentVariationReport();
					almtVarReport.alignment = alignment;
					almtVarReport.variation = GlueDataObject.lookup(cmdContext, Variation.class, memberCount.getVariationPkMap());
					almtVarReport.frequency = frequency;
					almtVarReport.totalPresent = memberCount.getMembersWherePresent();
					almtVarReport.totalAbsent = memberCount.getMembersWhereAbsent();
					almtVarReports.add(almtVarReport);

					if(almtVarReports.size() % 500 == 0) {
						log("Generated "+almtVarReports.size()+" alignment-variation frequencies");
					}
				}
			}
		}
		return almtVarReports;
	}

	public void generateVarAlmtNotes(CommandContext cmdContext, List<VariationFrequenciesGenerator.AlignmentVariationReport> almtVarReports) {
		int i = 0;
		for(AlignmentVariationReport almtVarReport: almtVarReports) {
			createOrUpdateVarAlmtNote(cmdContext, almtVarReport);
			i++;
			if(i % 1000 == 0) {
				cmdContext.commit();
				log("Created/updated "+i+" variation-alignment notes");
			}
		}
		if(i > 0) {
			cmdContext.commit();
			log("Created/updated "+i+" variation-alignment notes");
		}
	}
	
	private void createOrUpdateVarAlmtNote(CommandContext cmdContext,
			VariationFrequenciesGenerator.AlignmentVariationReport almtVarReport) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		VarAlmtNote varAlmtNote = GlueDataObject.lookup(cmdContext, VarAlmtNote.class, 
				VarAlmtNote.pkMap(almtVarReport.alignment.getName(), 
						almtVarReport.variation.getFeatureLoc().getReferenceSequence().getName(), 
						almtVarReport.variation.getFeatureLoc().getFeature().getName(), 
						almtVarReport.variation.getName()), true);
		if(varAlmtNote == null) {
			varAlmtNote = VariationCreateAlmtNoteCommand.createAlmtNote(cmdContext, 
					almtVarReport.variation, almtVarReport.alignment);
		}
		PropertyCommandDelegate.executeSetField(cmdContext, insideProjectMode.getProject(), 
				ConfigurableTable.var_almt_note.name(), varAlmtNote, frequencyFieldName, 
				new Double(almtVarReport.frequency), true);
		PropertyCommandDelegate.executeSetField(cmdContext, insideProjectMode.getProject(), 
				ConfigurableTable.var_almt_note.name(), varAlmtNote, totalPresentFieldName, 
				new Integer(almtVarReport.totalPresent), true);
		PropertyCommandDelegate.executeSetField(cmdContext, insideProjectMode.getProject(), 
				ConfigurableTable.var_almt_note.name(), varAlmtNote, totalAbsentFieldName, 
				new Integer(almtVarReport.totalAbsent), true);
		cmdContext.cacheUncommitted(varAlmtNote);
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		insideProjectMode.getProject().checkProperty(ConfigurableTable.var_almt_note.name(), frequencyFieldName, FieldType.DOUBLE, true);
	}
	
	
	
	
	
}
