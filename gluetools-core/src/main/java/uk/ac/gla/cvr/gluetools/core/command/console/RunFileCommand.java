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
package uk.ac.gla.cvr.gluetools.core.command.console;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;


@CommandClass(
	commandWords={"run", "file"},
	docoptUsages={"[-E] [-C] [-O] <filePath>"},
	docoptOptions={
			"-E, --no-cmd-echo      Suppress batch command echo",
			"-C, --no-comment-echo  Suppress batch comment echo",
	   		"-O, --no-output        Suppress batch result output"},
	description="Run batch commands from a file",
	metaTags = { CmdMeta.consoleOnly}
) 
public class RunFileCommand extends Command<OkResult> {

	
	private String filePath;
	private boolean noCmdEcho;
	private boolean noCommentEcho;
	private boolean noOutput;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.filePath = PluginUtils.configureStringProperty(configElem, "filePath", true);
		this.noCmdEcho = PluginUtils.configureBooleanProperty(configElem, "no-cmd-echo", true);
		this.noCommentEcho = PluginUtils.configureBooleanProperty(configElem, "no-comment-echo", true);
		this.noOutput = PluginUtils.configureBooleanProperty(configElem, "no-output", true);
	}



	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		long startTime = System.currentTimeMillis();
		GlueLogger.getGlueLogger().finest("Started running GLUE batch "+filePath);
		String batchContent = new String(consoleCmdContext.loadBytes(filePath));
		consoleCmdContext.runBatchCommands(filePath, batchContent, noCmdEcho, noCommentEcho, noOutput);
		long milliseconds = System.currentTimeMillis()-startTime;
		GlueLogger.getGlueLogger().finest("Completed GLUE batch "+filePath+", time taken: "+DateUtils.formatDuration(milliseconds));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("filePath", false);
		}
	}
}
