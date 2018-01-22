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

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

@CommandClass( 
		commandWords = {"glue-engine","show-version"},
		docoptUsages = {""}, 
		description = "Show the GLUE engine and database schema versions")
public class GlueEngineShowVersionCommand extends Command<MapResult> {

	@Override
	public MapResult execute(CommandContext cmdContext) {
		String glueEngineVersion = cmdContext.getGluetoolsEngine().getGluecoreProperties().getProperty("version", null);
		String dbSchemaVersion = cmdContext.getGluetoolsEngine().getDbSchemaVersion();		
		return new GlueEngineShowVersionResult(glueEngineVersion, dbSchemaVersion);
	}

	
	public static class GlueEngineShowVersionResult extends MapResult {

		public GlueEngineShowVersionResult(String glueEngineVersion, String dbSchemaVersion) {
			super("glueEngineShowVersionResult", mapBuilder()
					.put("glueEngineVersion", glueEngineVersion)
					.put("dbSchemaVersion", dbSchemaVersion));
		}
		
	}
	
}
