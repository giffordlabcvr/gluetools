package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class NcbiImporterSummaryResult extends MapResult {

	public NcbiImporterSummaryResult(NcbiImporterStatus ncbiImporterStatus) {
		super("ncbiImporterSummaryResult", mapBuilder()
				.put("totalMatching", ncbiImporterStatus.getTotalMatching())
				.put("present", ncbiImporterStatus.getPresentGiNumbers().size())
				.put("surplus", ncbiImporterStatus.getSurplusGiNumbers().size())
				.put("missing", ncbiImporterStatus.getMissingGiNumbers().size())
				.put("deleted", ncbiImporterStatus.getDeletedGiNumbers().size())
				.put("downloaded", ncbiImporterStatus.getDownloadedGiNumbers().size()));
	}

	
}
