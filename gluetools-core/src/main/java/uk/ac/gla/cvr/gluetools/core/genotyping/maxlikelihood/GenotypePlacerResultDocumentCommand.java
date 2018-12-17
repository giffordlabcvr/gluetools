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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"genotype", "placer-result-document"}, 
		description = "Generate genotyping results from a placer result document", 
		docoptUsages = { },
		metaTags = {CmdMeta.inputIsComplex}	
)
public class GenotypePlacerResultDocumentCommand extends BaseGenotypePlacerResultCommand {

	public final static String PLACER_RESULT_DOCUMENT = "placerResultDocument";
	
	private CommandDocument placerResultCmdDoc;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.placerResultCmdDoc = PluginUtils.configureCommandDocumentProperty(configElem, PLACER_RESULT_DOCUMENT, true);
	}

	
	@Override
	protected CommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper) {
		return super.executeOnPlacerResultDocument(cmdContext, maxLikelihoodGenotyper, placerResultCmdDoc);
	}


	
}
