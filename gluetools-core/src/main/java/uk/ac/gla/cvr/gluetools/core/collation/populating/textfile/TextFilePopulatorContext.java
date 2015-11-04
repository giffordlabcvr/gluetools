package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;

public class TextFilePopulatorContext {
	Map<Integer, TextFilePopulatorColumn> positionToColumn = null;
	Map<TextFilePopulatorColumn, Integer> columnToPosition = null;
	ConsoleCommandContext cmdContext;
	
}