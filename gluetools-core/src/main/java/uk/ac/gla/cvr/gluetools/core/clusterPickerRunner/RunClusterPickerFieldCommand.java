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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.AbstractStringAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.ExplicitMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(commandWords={"run", "cluster-picker", "field"},
	docoptUsages="-a <alignmentName> [-d <dataDir>]",
	docoptOptions={
		"-a <alignmentName>, --alignmentName <alignmentName>  Stored alignment name",
		"-d <dataDir>, --dataDir <dataDir>                    Directory to store intermediate data files",
	},
	description = "Run ClusterPicker based on a tree annotated as a field of a stored alignment", 
	furtherHelp = "The name of the field is given as the phyloFieldName property of the clusterPickerRunner module.", 
	metaTags = { CmdMeta.consoleOnly } )
public class RunClusterPickerFieldCommand extends ModulePluginCommand<ClusterPickerResult, ClusterPickerRunner> {

	private String alignmentName;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, "dataDir", false);
	}

	@Override
	protected ClusterPickerResult execute(CommandContext cmdContext, ClusterPickerRunner clusterPickerRunner) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(this.alignmentName));
		boolean recursive;
		if(alignment.isConstrained()) {
			recursive = true;
		} else {
			recursive = false;
		}
		PhyloTree phyloTree = PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, clusterPickerRunner.getPhyloFieldName(), recursive);
		List<Map<String, String>> memberPkMaps = new ArrayList<Map<String, String>>();
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
				memberPkMaps.add(memberPkMap);
			}
		});
		ExplicitMemberSupplier queryMemberSupplier = new ExplicitMemberSupplier(alignment.getName(), memberPkMaps);
		
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
		return new ClusterPickerResult(clusterPickerRunner.runClusterPicker(cmdContext, alignmentMap, phyloTree, dataDirFile));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("dataDir", true);
		}
		
	}
	
}
