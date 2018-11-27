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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass(
		commandWords={"export", "placement-from-document", "phylogeny"}, 
		description = "Export phylogeny file for a single placement in a result document", 
		docoptUsages = {},
		furtherHelp = "The reference phylogeny will be output, with an additional leaf node, representing the specified placement."+
				"If <leafName> is specified, this will annotate the new placement. Otherwise, the query sequence name will be used.",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class ExportPlacementPhylogenyFromDocumentCommand extends BaseExportPlacementPhylogenyCommand<PhyloTreeResult> {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		configureResultDocument(pluginConfigContext, configElem);
	}

	@Override
	protected PhyloTreeResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		return executeBasedOnDocument(cmdContext, maxLikelihoodPlacer);
	}

	@Override
	protected PhyloTreeResult executeOnPlacementResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			IMaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement) {
		PhyloTree glueProjectPhyloTree = super.generatePhyloTree(cmdContext, maxLikelihoodPlacer, placerResult, queryResult, placement);
		return new PhyloTreeResult(glueProjectPhyloTree);
	}
	
	
}
