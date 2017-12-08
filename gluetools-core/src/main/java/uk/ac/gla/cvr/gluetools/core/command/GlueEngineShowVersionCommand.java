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
