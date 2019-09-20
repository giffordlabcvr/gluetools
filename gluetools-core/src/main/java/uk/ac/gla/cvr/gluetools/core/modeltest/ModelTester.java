/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.modeltest;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;import org.w3c.dom.Element;

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
