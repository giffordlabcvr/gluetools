package uk.ac.gla.cvr.gluetools.core.modeltest;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.jmodeltest.JModelTestRunner;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="modelTester",
		includeInWebDocs=false)
public class ModelTester extends ModulePlugin<ModelTester> {

	private static final String J_MODEL_TEST_RUNNER = "jModelTestRunner";
	private JModelTestRunner jModelTestRunner = new JModelTestRunner();

	public ModelTester() {
		super();
		registerModulePluginCmdClass(TestModelsCommand.class);
		PropertyGroup jModelTestPropertyGroup = getRootPropertyGroup().addChild(J_MODEL_TEST_RUNNER);
		jModelTestRunner.configurePropertyGroup(jModelTestPropertyGroup);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element jModelTestRunnnerElem = PluginUtils.findConfigElement(configElem, J_MODEL_TEST_RUNNER);
		if(jModelTestRunnnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, jModelTestRunnnerElem, jModelTestRunner);
		}
	}

	
	public TestModelsResult testModels(CommandContext cmdContext,
			Map<Map<String, String>, DNASequence> nucleotideAlignment, String dataDir) {
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);

		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, DNASequence> remappedNtAlignment = FastaUtils.remapFasta(
				nucleotideAlignment, rowNameToMemberPkMap, memberPkMapToRowName, "M");

		jModelTestRunner.runJModelTest(cmdContext, remappedNtAlignment, dataDirFile);

		TestModelsResult testModelsResult = new TestModelsResult();
		return testModelsResult;
	}
	
}
