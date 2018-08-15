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
package uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.GenerateAminoAcidPhylogenyCommand;
import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.GeneratePhylogenyCommand.PhylogenyCommandCompleter;
import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml.RaxmlPhylogenyException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.phylogeny.RaxmlPhylogenyResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.phylogeny.RaxmlPhylogenyRunner;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass( 
		commandWords={"generate", "amino-acid", "phylogeny"}, 
		docoptUsages={"<alignmentName> (-s <selectorName> | -r <relRefName> -f <featureName>) [-c] (-w <whereClause> | -a) -o <outputFile> <outputFormat>  [-d <dataDir>]"},
		docoptOptions={
				"-s <selectorName>, --selectorName <selectorName>  Column selector module name",
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify members",
		    "-a, --allMembers                                      All members",
			"-o <outputFile>, --outputFile <outputFile>            Phylogeny output file",
			"-d <dataDir>, --dataDir <dataDir>                     Save algorithmic data in this directory"},
		metaTags = { CmdMeta.consoleOnly },
		description="Generate RAxML phylogeny using an amino-acid alignment", 
		furtherHelp="If <outputFile> is supplied, the Newick string is saved to an output file.\n"+
			"If <fieldName> is supplied, the Newick string is set as the phylogeny on the relevant field of the alignment table.\n"+
			"If supplied, <dataDir> must either not exist or be an empty directory.") 
public class GenerateRaxmlAminoAcidPhylogenyCommand extends GenerateAminoAcidPhylogenyCommand<RaxmlPhylogenyGenerator> {

	public final static String DATA_DIR = "dataDir";
	
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}


	@Override
	protected PhyloTree generateAminoAcidPhylogeny(CommandContext cmdContext,
			RaxmlPhylogenyGenerator modulePlugin, 
			Map<Map<String, String>, ProteinSequence> memberPkMapToAlignmentRow) {
		RaxmlPhylogenyRunner raxmlPhylogenyRunner = modulePlugin.getRaxmlPhylogenyRunner();
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);

		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, ProteinSequence> alignment = FastaUtils.remapFasta(
				memberPkMapToAlignmentRow, rowNameToMemberPkMap, memberPkMapToRowName, "M");

		RaxmlPhylogenyResult raxmlPhylogenyResult = 
				raxmlPhylogenyRunner.executeRaxmlAminoAcidPhylogeny(cmdContext, alignment, dataDirFile);
		PhyloTree phyloTree = raxmlPhylogenyResult.getPhyloTree();
		
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();

		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				Map<String, String> memberPkMap = rowNameToMemberPkMap.get(leafName);
				if(memberPkMap == null) {
					throw new RaxmlPhylogenyException(Code.UNKNOWN_LEAF_NAME, leafName);
				}
				phyloLeaf.setName(project.pkMapToTargetPath(ConfigurableTable.alignment_member.name(), memberPkMap));
			}
		});
		
		return phyloTree;
	}

	@CompleterClass
	public static final class Completer extends PhylogenyCommandCompleter {
		public Completer() {
			super();
			registerPathLookup("dataDir", true);
		}
		
	}

}
