package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class MemberQuerySequenceSupplier extends AbstractSequenceSupplier {

	private String almtName;
	private boolean recursive;
	private Optional<Expression> whereClause;


	public MemberQuerySequenceSupplier(String almtName, boolean recursive, Optional<Expression> whereClause) {
		super();
		this.almtName = almtName;
		this.recursive = recursive;
		this.whereClause = whereClause;
	}

	@Override
	public int countSequences(CommandContext cmdContext) {
		return AlignmentListMemberCommand.countMembers(cmdContext, supplyAlignment(cmdContext), recursive, whereClause);
	}

	@Override
	public List<Sequence> supplySequences(CommandContext cmdContext, int offset, int number) {
		Alignment alignment = supplyAlignment(cmdContext);
		return AlignmentListMemberCommand
				.listMembers(cmdContext, alignment, recursive, whereClause, offset, number, number)
				.stream()
				.map(almtMember -> almtMember.getSequence())
				.collect(Collectors.toList());
	}

	private Alignment supplyAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(this.almtName), false);
	}
	

}
