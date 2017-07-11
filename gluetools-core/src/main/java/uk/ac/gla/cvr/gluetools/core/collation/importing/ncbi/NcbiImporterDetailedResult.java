package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterStatus.SequenceStatus;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class NcbiImporterDetailedResult extends BaseTableResult<NcbiImporterStatus.SequenceStatus> {

	public NcbiImporterDetailedResult(List<SequenceStatus> sequenceStatusTable) {
		super("ncbiImporterDetailedResult", sequenceStatusTable, 
				column("giNumber", SequenceStatus::getGiNumber), 
				column("sequenceID", SequenceStatus::getSequenceID), 
				column("status", seqStatus -> seqStatus.getStatus().name()), 
				column("action", seqStatus -> 
					Optional.ofNullable(seqStatus.getAction())
					.map(a -> a.name()).orElse(null)));
	}

}
