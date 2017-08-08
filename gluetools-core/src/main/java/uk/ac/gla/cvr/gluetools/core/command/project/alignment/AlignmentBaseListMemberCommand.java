package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand.AbstractListCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AlignmentBaseListMemberCommand<R extends CommandResult> extends AlignmentModeCommand<R> {

	public static final String RECURSIVE = "recursive";
	
	private AbstractListCTableDelegate listCTableDelegate = new AbstractListCTableDelegate();
	
	private Boolean recursive;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		listCTableDelegate.setTableName(ConfigurableTable.alignment_member.name());
		listCTableDelegate.configure(pluginConfigContext, configElem);
	}

	
	protected AbstractListCTableDelegate getListCTableDelegate() {
		return listCTableDelegate;
	}


	protected Boolean getRecursive() {
		return recursive;
	}

	
	private static void checkListMemberOptions(Alignment alignment, Boolean recursive) {
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence == null && recursive) {
			throw new AlignmentException(Code.CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}
	}

	public static List<AlignmentMember> listMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Optional<Expression> whereClause) {
		return listMembers(cmdContext, alignment, recursive, whereClause, null, null, null);
	}

	
	public static int countMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Optional<Expression> whereClause) {
		checkListMemberOptions(alignment, recursive);
		
		Expression matchExpression = getMatchExpression(alignment, recursive, whereClause);

		SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, matchExpression);

		return GlueDataObject.count(cmdContext, selectQuery);
	}

	public static int countMembers(CommandContext cmdContext, String almtName,
			Boolean recursive, Optional<Expression> whereClause) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
		return countMembers(cmdContext, alignment, recursive, whereClause);
	}

	
	
	public static List<AlignmentMember> listMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Optional<Expression> whereClause,
			Integer offset, Integer fetchLimit, Integer pageSize) {
		checkListMemberOptions(alignment, recursive);
		
		Expression matchExpression = getMatchExpression(alignment, recursive, whereClause);

		SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, matchExpression);
		if(offset != null) {
			selectQuery.setFetchOffset(offset);
		}
		if(fetchLimit != null) {
			selectQuery.setFetchLimit(fetchLimit);
		}
		if(pageSize != null) {
			selectQuery.setPageSize(pageSize);
		}
		return GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
	}


	public static Expression getMatchExpression(Alignment alignment, Boolean recursive, Optional<Expression> whereClause) {
		Expression matchAlignmentOrDescendent = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName());
		if(recursive) {
			List<Alignment> descendents = alignment.getDescendents();
			for(Alignment descAlignment: descendents) {
				String descName = descAlignment.getName();
				matchAlignmentOrDescendent = matchAlignmentOrDescendent.orExp(
						ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, descName));
			}
		}
		Expression matchExpression;
		if(whereClause.isPresent()) {
			matchExpression = whereClause.get().andExp(matchAlignmentOrDescendent);
		} else {
			matchExpression = matchAlignmentOrDescendent;
		}
		return matchExpression;
	}
	
}
