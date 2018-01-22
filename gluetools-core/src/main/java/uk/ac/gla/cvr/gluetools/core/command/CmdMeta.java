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
		/** command may not be executed in a single line within a mode. */
		suppressDocs = "suppressDocs",
		/** command consumes binary data as part of its input.
		 *  this binary data must be provided in Base64 format using the property name specified in Command.BINARY_INPUT_PROPERTY */
		consumesBinary = "consumesBinary";
}
