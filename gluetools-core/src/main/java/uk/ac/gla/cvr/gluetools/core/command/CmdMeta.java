package uk.ac.gla.cvr.gluetools.core.command;

/*
 * Meta-tags for commands
 */
public class CmdMeta {

	public static final String 
		/** 
		 * the input schema for the command is too complex to
		 * be represented by docoptUsage, so the command can only be run programmatically, 
		 * not from the command line. */
		inputIsComplex = "inputIsComplex",
		/** command may only be executed via the console */
		consoleOnly = "consoleOnly",
		/** command may only be executed via the web API */
		webApiOnly = "webApiOnly",
		/** command makes updates to the database */
		updatesDatabase = "updatesDatabase",
		/** command may not be executed in a single line within a mode. */
		nonModeWrappable = "nonModeWrappable",
		/** command consumes binary data as part of its input.
		 *  this binary data must be provided in Base64 format using the property name specified in Command.BINARY_INPUT_PROPERTY */
		consumesBinary = "consumesBinary";
}
