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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseExportPlacementPhylogenyCommand<R extends CommandResult> extends AbstractPlacementCommand<R> {

	public static final String LEAF_NAME = "leafName";

	public static final String PLACEMENT_LEAF_PROPERTIES = "placementLeafProperty";
	public static final String PLACEMENT_BRANCH_PROPERTIES = "placementBranchProperty";

	private String leafName;
	
	private List<String> placementLeafProperties;
	private List<String> placementBranchProperties;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.leafName = PluginUtils.configureStringProperty(configElem, LEAF_NAME, false);
		this.placementLeafProperties = PluginUtils.configureStringsProperty(configElem, PLACEMENT_LEAF_PROPERTIES);
		this.placementBranchProperties = PluginUtils.configureStringsProperty(configElem, PLACEMENT_BRANCH_PROPERTIES);
	}
	
	protected PhyloTree generatePhyloTree(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			IMaxLikelihoodPlacerResult placerResult,
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
		this.placementLeafProperties.forEach(placementLeafProperty -> {
			if(placementLeafProperty.length() == 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Empty placementLeafProperty");
			}
			int firstColonIndex = placementLeafProperty.indexOf(':');
			if(firstColonIndex < 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "placementLeafProperty string does not contain ':'");
			}
			String key = placementLeafProperty.substring(0, firstColonIndex);
			String value = placementLeafProperty.substring(firstColonIndex+1);
			placementLeaf.ensureUserData().put(key, value);
		});
		PhyloBranch placementBranch = placementLeaf.getParentPhyloBranch();
		this.placementBranchProperties.forEach(placementBranchProperty -> {
			if(placementBranchProperty.length() == 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Empty placementBranchProperty");
			}
			int firstColonIndex = placementBranchProperty.indexOf(':');
			if(firstColonIndex < 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "placementBranchProperty string does not contain ':'");
			}
			String key = placementBranchProperty.substring(0, firstColonIndex);
			String value = placementBranchProperty.substring(firstColonIndex+1);
			placementBranch.ensureUserData().put(key, value);
		});
		
		return glueProjectPhyloTree;
	}
	
	
}
