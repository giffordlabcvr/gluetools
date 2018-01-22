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
package uk.ac.gla.cvr.gluetools.core.treetransformer;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"transform-tree", "newick"}, 
		description = "Rename leaf nodes in a Newick tree file", 
		docoptUsages = { "-i <inputFile> -o <outputFile>"},
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     Input newick file",
				"-o <outputFile>, --outputFile <outputFile>  Output newick file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class TransformTreeCommand extends ModulePluginCommand<OkResult, TreeTransformer> {

	public static String INPUT_FILE = "inputFile";
	public static String OUTPUT_FILE = "outputFile";

	private String inputFile;
	private String outputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, TreeTransformer treeTransformer) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		NewickToPhyloTreeParser phyloTreeParser = new NewickToPhyloTreeParser();
		PhyloTree inputTree = phyloTreeParser.parseNewick(new String(consoleCmdContext.loadBytes(inputFile)));
		PhyloTree outputTree = treeTransformer.transformTree(cmdContext, inputTree);
		PhyloTreeToNewickGenerator newickPhyloTreeVisitor = new PhyloTreeToNewickGenerator();
		outputTree.accept(newickPhyloTreeVisitor);
		consoleCmdContext.saveBytes(outputFile, newickPhyloTreeVisitor.getNewickString().getBytes());
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
			registerPathLookup("outputFile", false);
		}
		
	}
	
}
