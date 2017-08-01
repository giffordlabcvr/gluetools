package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public abstract class AbstractMemberSupplier {

	private String almtName;

	public AbstractMemberSupplier(String almtName) {
		this.almtName = almtName;
	}

	public final Alignment supplyAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(this.almtName), false);
	}
	
	public abstract int countMembers(CommandContext cmdContext);
	
	public abstract List<AlignmentMember> supplyMembers(CommandContext cmdContext, int offset, int number);
	
}
