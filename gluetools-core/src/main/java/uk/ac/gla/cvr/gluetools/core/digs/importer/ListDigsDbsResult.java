package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;

public class ListDigsDbsResult extends ListResult {

	public static final String DIGS_DB_NAME = "digsDbName";

	public ListDigsDbsResult(CommandContext cmdContext, List<String> dbList) {
		super(cmdContext, String.class, dbList, Arrays.asList(DIGS_DB_NAME), new BiFunction<String, String, Object>() {
			@Override
			public Object apply(String data, String header) {
				if(header.equals(DIGS_DB_NAME)) {
					return data;
				}
				return null;
			}
			
		});
	}

}
