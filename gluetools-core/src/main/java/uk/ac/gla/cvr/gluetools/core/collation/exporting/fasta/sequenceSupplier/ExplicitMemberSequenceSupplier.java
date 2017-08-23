package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class ExplicitMemberSequenceSupplier extends AbstractSequenceSupplier {

	private List<Map<String,String>> memberPkMaps;
	
	public ExplicitMemberSequenceSupplier(List<Map<String, String>> memberPkMaps) {
		super();
		this.memberPkMaps = memberPkMaps;
	}

	@Override
	public int countSequences(CommandContext cmdContext) {
		return memberPkMaps.size();
	}

	@Override
	public List<Sequence> supplySequences(CommandContext cmdContext, int offset, int number) {
		List<Map<String,String>> subList = memberPkMaps.subList(offset, Math.min(offset+number, memberPkMaps.size()));
		return subList.stream()
				.map(pkMap -> GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap).getSequence())
				.collect(Collectors.toList());
	}

}
