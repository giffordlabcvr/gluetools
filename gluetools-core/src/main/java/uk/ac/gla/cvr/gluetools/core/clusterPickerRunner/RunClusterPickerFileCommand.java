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
package uk.ac.gla.cvr.gluetools.core.clusterPickerRunner;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.clusterPickerRunner.ClusterPickerException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractStringAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(commandWords={"run", "cluster-picker", "file"},
	docoptUsages="-a <alignmentName> [-w <whereClause>] -t <treeFileName> <treeFileFormat> [-d <dataDir>]",
	docoptOptions={
		"-t <treeFileName>, --treeFileName <treeFileName>     Phylogenetic tree file path",
		"-a <alignmentName>, --alignmentName <alignmentName>  Stored alignment name",
		"-w <whereClause>, --whereClause <whereClause>        Qualifier for leaf nodes",
		"-d <dataDir>, --dataDir <dataDir>                    Directory to store intermediate data files",
	},
	description = "Run ClusterPicker based on a tree file and stored alignment", 
	furtherHelp = "If <whereClause> is supplied, the tree leaf set must correspond to the set of alignment "+
			"members selected by the <whereClause>. Otherwise, it must correspond to the set of all alignment "+
			"members.", 
	metaTags = { CmdMeta.consoleOnly } )
public class RunClusterPickerFileCommand extends ModulePluginCommand<ClusterPickerResult, ClusterPickerRunner> {

	private String alignmentName;
	private Optional<Expression> whereClause;
	private String treeFileName;
	private PhyloFormat treeFileFormat;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
		this.treeFileName = PluginUtils.configureStringProperty(configElem, "treeFileName", true);
		this.treeFileFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, "treeFileFormat", true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, "dataDir", false);
	}

	@Override
	protected ClusterPickerResult execute(CommandContext cmdContext, ClusterPickerRunner clusterPickerRunner) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(this.alignmentName, false, this.whereClause);
		
		Map<String, DNASequence> alignmentMap = new LinkedHashMap<String, DNASequence>();
		
		FastaAlignmentExporter.exportAlignment(cmdContext, null, false, 
				queryMemberSupplier, new AbstractStringAlmtRowConsumer() {
					@Override
					public void consumeAlmtRow(CommandContext cmdContext,
							AlignmentMember almtMember, String alignmentRowString) {
						String key = project.pkMapToTargetPath(ConfigurableTable.alignment_member.name(), almtMember.pkMap());
						DNASequence dnaSequence = FastaUtils.ntStringToSequence(alignmentRowString);
						alignmentMap.put(key, dnaSequence);
					}
		});
		
		Set<String> selectedMemberNames = alignmentMap.keySet();
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		PhyloTree phyloTree = treeFileFormat.parse(consoleCmdContext.loadBytes(treeFileName));
		Set<String> treeLeafNames = new LinkedHashSet<String>();
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				treeLeafNames.add(phyloLeaf.getName());
			}
		});
		selectedMemberNames.forEach(smn -> {
			if(!treeLeafNames.contains(smn)) {
				throw new ClusterPickerException(Code.ALIGNMENT_MEMBER_NOT_IN_TREE, smn);
			}
		});
		treeLeafNames.forEach(tln -> {
			if(!selectedMemberNames.contains(tln)) {
				throw new ClusterPickerException(Code.TREE_LEAF_NOT_IN_ALIGNMENT, tln);
			}
		});
		return new ClusterPickerResult(clusterPickerRunner.runClusterPicker(cmdContext, alignmentMap, phyloTree, dataDirFile));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("treeFileName", false);
			registerEnumLookup("treeFileFormat", PhyloFormat.class);
			registerPathLookup("dataDir", true);
		}
		
	}
	
}
