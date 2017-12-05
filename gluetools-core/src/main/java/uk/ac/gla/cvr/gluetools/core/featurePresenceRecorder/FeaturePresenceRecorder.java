package uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

/*
 * Module which generates memberFLocNotes based on the presence of a 
 * given feature in a given alignment member.
 */
@PluginClass(elemName="featurePresenceRecorder",
		description="Generates MemberFLocNote objects based on the presence of a given Feature amongst AlignmentMembers")
public class FeaturePresenceRecorder extends ModulePlugin<FeaturePresenceRecorder> {

	
	public static String REF_NT_COVERAGE_FIELD_NAME = "refNtCoverageFieldName";
	public static String MIN_REF_NT_COVERAGE_PCT = "minRefNtCoveragePct";
	
	// name of a DOUBLE field on memberFLocNote, where the reference NT coverage percentage will be set.
	// default reference_nt_coverage_pct
	private String refNtCoverageFieldName; 
	// if reference NT coverage percentage is below this value, no memberFLocNote will be generated / updated.
	// default: 10.0%
	private Double minRefNtCoveragePct; 

	public FeaturePresenceRecorder() {
		super();
		registerModulePluginCmdClass(RecordFeaturePresenceCommand.class);
		addSimplePropertyName(REF_NT_COVERAGE_FIELD_NAME);
		addSimplePropertyName(MIN_REF_NT_COVERAGE_PCT);
	}

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.refNtCoverageFieldName = Optional
				.ofNullable(PluginUtils.configureStringProperty(configElem, REF_NT_COVERAGE_FIELD_NAME, false))
				.orElse("reference_nt_coverage_pct");
		this.minRefNtCoveragePct = 
				Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_REF_NT_COVERAGE_PCT, 0.0, true, 100.0, true, false))
				.orElse(10.0);

	}

	public String getRefNtCoverageFieldName() {
		return refNtCoverageFieldName;
	}

	public Double getMinRefNtCoveragePct() {
		return minRefNtCoveragePct;
	}
}
