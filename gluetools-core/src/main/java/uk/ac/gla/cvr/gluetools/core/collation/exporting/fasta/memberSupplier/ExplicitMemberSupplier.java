package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class ExplicitMemberSupplier extends AbstractMemberSupplier {

	private List<Map<String,String>> pkMaps;
	
	public ExplicitMemberSupplier(String almtName, List<Map<String, String>> pkMaps) {
		super(almtName);
		this.pkMaps = pkMaps;
	}

	@Override
	public int countMembers(CommandContext cmdContext) {
		return pkMaps.size();
	}

	@Override
	public List<AlignmentMember> supplyMembers(CommandContext cmdContext, int offset, int number) {
		List<Map<String,String>> subList = pkMaps.subList(offset, Math.min(offset+number, pkMaps.size()));
		return subList.stream()
				.map(pkMap -> GlueDataObject.lookup(cmdContext, AlignmentMember.class, pkMap))
				.collect(Collectors.toList());
	}

}
