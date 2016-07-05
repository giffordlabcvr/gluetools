package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class MemberCountAminoAcidResult extends MapResult {

	public MemberCountAminoAcidResult(String aminoAcid, int count) {
		super("memberCountAminoAcidResult", mapBuilder().put("aminoAcid", aminoAcid)
				.put("count", count));
	}

}
