package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand.AbstractListCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"list", "member"},
		docoptUsages={"[-r] [-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<fieldName> ...]"},
		docoptOptions={
				"-r, --recursive                                         Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
				"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
				"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"
			},
		description="List member sequences or field values",
		furtherHelp=
		"The optional whereClause qualifies which alignment member are displayed.\n"+
		"If whereClause is not specified, all alignment members are displayed.\n"+
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member -w \"sequence.source.name = 'local'\"\n"+
		"  list member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\"\n"+
		"  list member sequence.sequenceID sequence.custom_field"
	) 
public class AlignmentListMemberCommand extends AlignmentModeCommand<ListResult> {

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

	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		listCTableDelegate.setWhereClause(Optional.of(getMatchExpression(alignment, recursive, listCTableDelegate.getWhereClause())));
		return listCTableDelegate.execute(cmdContext);
	}

	
	private static void checkListMemberOptions(Alignment alignment, Boolean recursive) {
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence == null && recursive) {
			throw new AlignmentException(Code.CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}
	}

	// deduplicate: since sequences can be members of multiple alignments, in the recursive case the same sequence may appear
	// as the member of multiple descendents.
	// deduplicate will remove duplicate sequences, in favour of those closest in the hierarchy to the supplied alignment, 
	// breaking ties by sorting on alignment name.
	
	public static List<AlignmentMember> listMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Boolean deduplicate, Optional<Expression> whereClause) {
		checkListMemberOptions(alignment, recursive);
		
		Expression matchExpression = getMatchExpression(alignment, recursive, whereClause);

		Map<String, Integer> alignmentNameToDecOrder = new LinkedHashMap<String, Integer>();
		alignmentNameToDecOrder.put(alignment.getName(), 0);
		
		if(recursive) {
			int decOrder = 1;
			List<Alignment> descendents = alignment.getDescendents();
			for(Alignment descAlignment: descendents) {
				String descName = descAlignment.getName();
				alignmentNameToDecOrder.put(descName, decOrder);
				decOrder++;
			}
		}
		
		SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, matchExpression);
		List<AlignmentMember> result = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
		if(recursive && deduplicate) {
			List<AlignmentMember> membersSorted = new ArrayList<AlignmentMember>(result);
			// sort members so that those in higher up alignments are considered first during deduplication.
			Collections.sort(membersSorted, new Comparator<AlignmentMember>() {
				@Override
				public int compare(AlignmentMember o1, AlignmentMember o2) {
					String o1AlmtName = o1.getAlignment().getName();
					String o2AlmtName = o2.getAlignment().getName();
					int comp = Integer.compare(alignmentNameToDecOrder.get(o1AlmtName), alignmentNameToDecOrder.get(o2AlmtName));
					if(comp != 0) { return comp; }
					comp = o1AlmtName.compareTo(o2AlmtName);
					if(comp != 0) { return comp; }
					comp = o1.getSequence().getSource().getName().compareTo(o2.getSequence().getSource().getName());
					if(comp != 0) { return comp; }
					comp = o1.getSequence().getSequenceID().compareTo(o2.getSequence().getSequenceID());
					if(comp != 0) { return comp; }
					return 0;
				}
			});
			Set<Sequence> sequences = new LinkedHashSet<Sequence>();
			List<AlignmentMember> deduplicatedMembers = new ArrayList<AlignmentMember>();
			for(AlignmentMember member: membersSorted) {
				if(sequences.contains(member.getSequence())) {
					continue;
				}
				sequences.add(member.getSequence());
				deduplicatedMembers.add(member);
			}
			result = deduplicatedMembers;
		}
		return result;
	}


	static Expression getMatchExpression(Alignment alignment, Boolean recursive, Optional<Expression> whereClause) {
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

	
	@CompleterClass
	public static class Completer extends AbstractListCTableCommand.ListCommandCompleter {
		
		public Completer() {
			super(ConfigurableTable.alignment_member.name());
		}
	}
	
}
