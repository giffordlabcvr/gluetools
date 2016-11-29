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
