package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;

public class TextFilePopulatorContext {
	Map<Integer, List<BaseTextFilePopulatorColumn>> positionToColumn = null;
	Map<BaseTextFilePopulatorColumn, Integer> columnToPosition = null;
	ConsoleCommandContext cmdContext;
	
}