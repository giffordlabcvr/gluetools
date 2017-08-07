package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class ExplicitSequenceSupplier extends AbstractSequenceSupplier {

	private List<Map<String,String>> pkMaps;
	
	public ExplicitSequenceSupplier(List<Map<String, String>> pkMaps) {
		super();
		this.pkMaps = pkMaps;
	}

	@Override
	public int countSequences(CommandContext cmdContext) {
		return pkMaps.size();
	}

	@Override
	public List<Sequence> supplySequences(CommandContext cmdContext, int offset, int number) {
		List<Map<String,String>> subList = pkMaps.subList(offset, Math.min(offset+number, pkMaps.size()));
		return subList.stream()
				.map(pkMap -> GlueDataObject.lookup(cmdContext, Sequence.class, pkMap))
				.collect(Collectors.toList());
	}

}
