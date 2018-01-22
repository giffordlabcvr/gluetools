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
package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public abstract class CommandUtils {

	public static <C extends GlueDataObject> ListResult runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query, List<String> fields) {
		
		List<C> resultDataObjects = GlueDataObject.query(cmdContext, theClass, query);
		return new ListResult(cmdContext, theClass, resultDataObjects, fields);
	}

	public static <C extends GlueDataObject> ListResult runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query) {
		
		List<C> resultDataObjects = GlueDataObject.query(cmdContext, theClass, query);
		return new ListResult(cmdContext, theClass, resultDataObjects);
	}


	public static <C extends GlueDataObject> ListResult runListCommand(
			CommandContext cmdContext, Class<C> theClass, Comparator<C> sortComparator, SelectQuery query) {
		List<C> resultDataObjects = GlueDataObject.query(cmdContext, theClass, query);
		Collections.sort(resultDataObjects, sortComparator);
		return new ListResult(cmdContext, theClass, resultDataObjects);
	}

	public static File ensureDataDir(CommandContext cmdContext, String dataDirString) {
		File dataDirFile = null;
		if(dataDirString != null) {
			if(!(cmdContext instanceof ConsoleCommandContext)) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "The <dataDir> option is only usable in console mode.");
			}
			ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
			boolean finalised = false;
			Integer suffix = 1;
			String dirStringToUse = dataDirString;
			while(!finalised) {
				dataDirFile = consoleCommandContext.fileStringToFile(dirStringToUse);
				if(dataDirFile.exists() && (!dataDirFile.isDirectory() || dataDirFile.list().length > 0)) {
					if(suffix == null) {
						suffix = 1;
					} else {
						suffix = suffix + 1;
					}
					dirStringToUse = dataDirString+suffix;
				} else {
					finalised = true;
				}
			}
			if(!dirStringToUse.equals(dataDirString)) {
				GlueLogger.getGlueLogger().warning("Unable to use data directory "+dataDirString+"; using "+dirStringToUse+" instead");
			}
			if(!dataDirFile.exists()) {
				boolean mkdirsResult = dataDirFile.mkdirs();
				if(!mkdirsResult) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Failed to create directory: "+dataDirFile.getAbsolutePath());
				}
			}
		}
		return dataDirFile;
	}
}
