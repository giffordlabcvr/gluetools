package uk.ac.gla.cvr.gluetools.programs.jmodeltest;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

public class JModelTestRunner implements Plugin {

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
	}

	protected int getJModelTestCpus(CommandContext cmdContext) {
		return Integer.parseInt(cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_NUMBER_CPUS, "1"));
	}

	protected String getJModelTestJar(CommandContext cmdContext) {
		String jModelTestExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_JAR_PROPERTY);
		if(jModelTestExecutable == null) { throw new JModelTestException(JModelTestException.Code.JMODELTEST_CONFIG_EXCEPTION, "JModelTest executable not defined"); }
		return jModelTestExecutable;
	}

	protected String getJModelTestTempDir(CommandContext cmdContext) {
		String jModelTestTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_TEMP_DIR_PROPERTY);
		if(jModelTestTempDir == null) { throw new JModelTestException(JModelTestException.Code.JMODELTEST_CONFIG_EXCEPTION, "JModelTest temp directory not defined"); }
		return jModelTestTempDir;
	}

	
}
