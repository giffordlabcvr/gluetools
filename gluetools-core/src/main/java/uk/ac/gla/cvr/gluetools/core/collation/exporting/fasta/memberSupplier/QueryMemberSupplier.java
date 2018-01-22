/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
