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
package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"reformat-phylogeny"}, 
		description = "Translate a phylogenetic tree between file formats", 
		docoptUsages = { "-i <inputFile> <inputFormat> -o <outputFile> <outputFormat>"},
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     Input file",
				"-o <outputFile>, --outputFile <outputFile>  Output file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ReformatPhylogenyCommand extends PhyloUtilityCommand<OkResult> {

	public static final String INPUT_FILE = "inputFile";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String INPUT_FORMAT = "inputFormat";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String inputFile;
	private String outputFile;
	private PhyloFormat inputFormat;
	private PhyloFormat outputFormat;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		this.inputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, INPUT_FORMAT, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, PhyloUtility treeRerooter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		PhyloTree phyloTree = inputFormat.parse(consoleCmdContext.loadBytes(inputFile));
		consoleCmdContext.saveBytes(outputFile, outputFormat.generate(phyloTree));
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
			registerEnumLookup("inputFormat", PhyloFormat.class);
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}
		
	}
	
}
