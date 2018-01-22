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
package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"export", "placement", "phylogeny"}, 
		description = "Export phylogeny file for a single placement in a result file", 
		docoptUsages = { "-i <inputFile> -q <queryName> -p <placementIndex> [-l <leafName>] -o <outputFile> <outputFormat>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>                 Placement results file",
				"-q <queryName>, --queryName <queryName>                 Query sequence name",
				"-p <placementIndex>, --placementIndex <placementIndex>  Placement index",
				"-l <leafName>, --leafName <leafName>                    Name given to placement leaf",
				"-o <outputFile>, --outputFile <outputFile>              Phylogeny output file",
		},
		furtherHelp = "The reference phylogeny will be output, with an additional leaf node, representing the specified placement."+
				"If <leafName> is specified, this will annotate the new placement. Otherwise, the query sequence name will be used.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ExportPlacementPhylogenyCommand extends AbstractPlacementCommand<OkResult> {

	public static final String OUTPUT_FILE = "outputFile";
	public static final String LEAF_NAME = "leafName";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String outputFile;
	private PhyloFormat outputFormat;
	private String leafName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);
		this.leafName = PluginUtils.configureStringProperty(configElem, LEAF_NAME, false);
	}

	@Override
	protected OkResult executeOnPlacementResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement) {
		
		PhyloTree glueProjectPhyloTree = maxLikelihoodPlacer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);
		
		PhyloLeaf placementLeaf = MaxLikelihoodPlacer.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, queryResult, placement);
		if(leafName == null) {
			placementLeaf.setName(queryResult.queryName);
		} else {
			placementLeaf.setName(leafName);
		}
		((ConsoleCommandContext) cmdContext).saveBytes(outputFile, outputFormat.generate(glueProjectPhyloTree));
		return new OkResult();
	}
	
	@CompleterClass
	public static class Completer extends AbstractPlacementCommandCompleter {
		public Completer() {
			super();
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}
		
	}


	
	
}
