package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhyloImporter.AlignmentPhylogeny;

public class ImportPhylogenyResult extends BaseTableResult<PhyloImporter.AlignmentPhylogeny> {

	public ImportPhylogenyResult(List<AlignmentPhylogeny> alignmentPhylogenies) {
		super("importPhylogenyResult", alignmentPhylogenies, 
				column("alignment", alPhyl -> alPhyl.getAlignment().getName()), 
				column("memberLeafNodes", alPhyl -> alPhyl.getMemberLeafNodes()), 
				column("pointerLeafNodes", alPhyl -> alPhyl.getPointerLeafNodes()), 
				column("internalNodes", alPhyl -> alPhyl.getInternalNodes()));
	}


}
