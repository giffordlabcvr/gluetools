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
package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"genotype", "placer-result"}, 
		description = "Generate genotyping results from a placer result file", 
		docoptUsages = { "-f <fileName> [-l <detailLevel> | -c]" },
		docoptOptions = { 
				"-f <fileName>, --fileName <fileName>           Placer result file path",
				"-l <detailLevel>, --detailLevel <detailLevel>  Table result detail level",
				"-c, --documentResult                           Output document rather than table result",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenotypePlacerResultCommand extends AbstractGenotypeCommand {

	public final static String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		maxLikelihoodGenotyper.log(Level.FINEST, "Reading placer result file "+fileName);
		byte[] placerResultBytes = consoleCommandContext.loadBytes(fileName);
		Document placerResultDocument = GlueXmlUtils.documentFromBytes(placerResultBytes);
		CommandDocument placerResultCmdDoc = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(placerResultDocument);
		MaxLikelihoodPlacerResult placerResult = PojoDocumentUtils.commandObjectToPojo(placerResultCmdDoc, MaxLikelihoodPlacerResult.class);

		MaxLikelihoodPlacer placer = maxLikelihoodGenotyper.resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = placer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);
		Map<String, QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, placerResult.singleQueryResult);
		return formResult(maxLikelihoodGenotyper, genotypeResults);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerEnumLookup("detailLevel", DetailLevel.class);
		}
	}

	
}
