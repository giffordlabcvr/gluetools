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
package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager.WebFileType;

@CommandClass(
		commandWords={"file-util", "save-string-web-file"}, 
		description = "Save a string to a web file", 
		docoptUsages = { "<string> <fileName>" },
		docoptOptions = {},
		metaTags = {CmdMeta.webApiOnly}	
)
public class FileUtilSaveStringWebFileCommand extends Command<CommandWebFileResult> {

	public static final String STRING = "string";
	public static final String FILENAME = "fileName";

	private String string;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.string = PluginUtils.configureStringProperty(configElem, STRING, true);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILENAME, true);
	}

	@Override
	public CommandWebFileResult execute(CommandContext cmdContext) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir(WebFileType.DOWNLOAD);
		webFilesManager.createWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName);

		try(OutputStream outputStream = webFilesManager.appendToWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName)) {
			IOUtils.write(string.getBytes(), outputStream);
		} catch(Exception e) {
			throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
		}
		String webFileSizeString = webFilesManager.getSizeString(WebFileType.DOWNLOAD, subDirUuid, fileName);
		
		return new CommandWebFileResult("stringWebFileResult", WebFileType.DOWNLOAD, subDirUuid, fileName, webFileSizeString);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
} 