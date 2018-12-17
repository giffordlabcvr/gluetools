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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class AbstractPlacerResultCommand<R extends CommandResult> extends ModulePluginCommand<R, MaxLikelihoodPlacer> {

	public final static String INPUT_FILE = "inputFile";

	private String inputFile;

	public void configureInputFile(PluginConfigContext pluginConfigContext, Element configElem) {
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
	}
	
	public final static String PLACER_RESULT_DOCUMENT = "placerResultDocument";
	
	private CommandDocument placerResultCmdDoc;
	
	public void configureResultDocument(PluginConfigContext pluginConfigContext, Element configElem) {
		this.placerResultCmdDoc = PluginUtils.configureCommandDocumentProperty(configElem, PLACER_RESULT_DOCUMENT, true);
	}

	protected final R executeBasedOnFile(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		ConsoleCommandContext consoleCmdContext = ((ConsoleCommandContext) cmdContext);
		IMaxLikelihoodPlacerResult placerResult = loadPlacerResult(consoleCmdContext, inputFile);
		return executeOnPlacerResult(cmdContext, maxLikelihoodPlacer, placerResult);
	}

	protected final R executeBasedOnDocument(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		IMaxLikelihoodPlacerResult placerResult = IMaxLikelihoodPlacerResult.fromCommandDocument(placerResultCmdDoc);
		return executeOnPlacerResult(cmdContext, maxLikelihoodPlacer, placerResult);
	}

	protected abstract R executeOnPlacerResult(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer, 
			IMaxLikelihoodPlacerResult placerResult);

	protected static IMaxLikelihoodPlacerResult loadPlacerResult(ConsoleCommandContext consoleCmdContext, String inputFile) {
		byte[] placerResultBytes = consoleCmdContext.loadBytes(inputFile);
		Document placerResultDocument = GlueXmlUtils.documentFromBytes(placerResultBytes);
		CommandDocument placerResultCmdDoc = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(placerResultDocument);
		IMaxLikelihoodPlacerResult placerResult = IMaxLikelihoodPlacerResult.fromCommandDocument(placerResultCmdDoc);
		return placerResult;
	}

	protected static class AbstractPlacerResultCommandCompleter extends AdvancedCmdCompleter {
		public AbstractPlacerResultCommandCompleter() {
			super();
			registerPathLookup("inputFile", false);
		}
	}

}
