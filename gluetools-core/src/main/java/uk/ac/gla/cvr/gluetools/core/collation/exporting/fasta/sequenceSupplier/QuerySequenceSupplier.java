package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public class QuerySequenceSupplier extends AbstractSequenceSupplier {

	private Optional<Expression> whereClause;
	
	public QuerySequenceSupplier(Optional<Expression> whereClause) {
		super();
		this.whereClause = whereClause;
	}

	@Override
	public int countSequences(CommandContext cmdContext) {
		SelectQuery selectQuery = getSelectQuery();
		return GlueDataObject.count(cmdContext, selectQuery);
	}

	private SelectQuery getSelectQuery() {
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		return selectQuery;
	}

	@Override
	public List<Sequence> supplySequences(CommandContext cmdContext, int offset, int number) {
		SelectQuery selectQuery = getSelectQuery();
		selectQuery.setFetchOffset(offset);
		selectQuery.setPageSize(number);
		selectQuery.setFetchLimit(number);
		return GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
	}

}
