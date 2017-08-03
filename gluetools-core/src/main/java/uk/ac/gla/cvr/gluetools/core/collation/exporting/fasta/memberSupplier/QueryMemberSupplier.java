package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class QueryMemberSupplier extends AbstractMemberSupplier {

	private boolean recursive;
	private Optional<Expression> whereClause;
	
	public QueryMemberSupplier(String almtName, boolean recursive, Optional<Expression> whereClause) {
		super(almtName);
		this.recursive = recursive;
		this.whereClause = whereClause;
	}

	@Override
	public int countMembers(CommandContext cmdContext) {
		return AlignmentListMemberCommand.countMembers(cmdContext, supplyAlignment(cmdContext), recursive, whereClause);
	}

	@Override
	public List<AlignmentMember> supplyMembers(CommandContext cmdContext, int offset, int number) {
		Alignment alignment = supplyAlignment(cmdContext);
		return AlignmentListMemberCommand.listMembers(cmdContext, alignment, recursive, whereClause, offset, number, number);
	}

}
