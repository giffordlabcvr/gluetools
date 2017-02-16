package uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.GenerateNucleotidePhylogenyCommand;
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
		commandWords={"generate", "nucleotide", "phylogeny"}, 
		docoptUsages={"<alignmentName> [-s <selectorName> | -r <relRefName> -f <featureName>] [-c] (-w <whereClause> | -a) [-i [-m <minColUsage>]] -o <outputFile> <outputFormat>  [-d <dataDir>]"},
		docoptOptions={
				"-s <selectorName>, --selectorName <selectorName>  Column selector module name",
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify members",
		    "-a, --allMembers                                      All members",
		    "-i, --includeAllColumns                               Include columns for all NTs",
		    "-m <minColUsage>, --minColUsage <minColUsage>         Minimum included column usage",
			"-o <outputFile>, --outputFile <outputFile>            Phylogeny output file",
			"-d <dataDir>, --dataDir <dataDir>                     Save algorithmic data in this directory"},
		metaTags = { CmdMeta.consoleOnly },
		description="Generate RAxML phylogeny using a nucleotide alignment", 
		furtherHelp="If <outputFile> is supplied, the Newick string is saved to an output file.\n"+
			"If <fieldName> is supplied, the Newick string is set as the phylogeny on the relevant field of the alignment table.\n"+
			"If supplied, <dataDir> must either not exist or be an empty directory.") 
public class GenerateRaxmlNucleotidePhylogenyCommand extends GenerateNucleotidePhylogenyCommand<RaxmlPhylogenyGenerator> {

	public final static String DATA_DIR = "dataDir";
	
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}


	@Override
	protected PhyloTree generatePhylogeny(CommandContext cmdContext,
			RaxmlPhylogenyGenerator modulePlugin, 
			Map<Map<String, String>, DNASequence> memberPkMapToAlignmentRow) {
		RaxmlPhylogenyRunner raxmlPhylogenyRunner = modulePlugin.getRaxmlPhylogenyRunner();
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);

		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, DNASequence> alignment = FastaUtils.remapFasta(
				memberPkMapToAlignmentRow, rowNameToMemberPkMap, memberPkMapToRowName, "M");

		RaxmlPhylogenyResult raxmlPhylogenyResult = raxmlPhylogenyRunner.executeRaxmlPhylogeny(cmdContext, alignment, dataDirFile);
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
