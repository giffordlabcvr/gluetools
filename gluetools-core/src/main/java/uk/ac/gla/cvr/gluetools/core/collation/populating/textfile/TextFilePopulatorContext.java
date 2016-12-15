package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;

public class TextFilePopulatorContext {
	Map<Integer, List<BaseTextFilePopulatorColumn>> positionToColumn = null;
	Map<BaseTextFilePopulatorColumn, Integer> columnToPosition = null;
	Optional<Expression> whereClause = null;
	ConsoleCommandContext cmdContext;
	List<Map<String,String>> results = new ArrayList<Map<String,String>>();
	Boolean updateDB;
	
}