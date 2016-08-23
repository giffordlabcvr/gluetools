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
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"list", "member"},
		docoptUsages={"[-r] [-w <whereClause>] [<fieldName> ...]"},
		docoptOptions={
				"-r, --recursive                                Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
			},
		description="List member sequences or field values",
		furtherHelp=
		"The optional whereClause qualifies which alignment member are displayed.\n"+
		"If whereClause is not specified, all alignment members are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member -w \"sequence.source.name = 'local'\"\n"+
		"  list member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\"\n"+
		"  list member sequence.sequenceID sequence.custom_field"
	) 
public class AlignmentListMemberCommand extends AlignmentModeCommand<ListResult> {

	public static final String RECURSIVE = "recursive";
	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}

	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		if(fieldNames != null) {
			getAlignmentMode(cmdContext).getProject().checkListableMemberField(fieldNames);
		}

		List<AlignmentMember> members = listMembers(cmdContext, lookupAlignment(cmdContext), recursive, false, whereClause);
		if(fieldNames == null) {
			return new ListResult(cmdContext, AlignmentMember.class, members);
		} else {
			return new ListResult(cmdContext, AlignmentMember.class, members, fieldNames);
		}
	}


	// deduplicate: since sequences can be members of multiple alignments, in the recursive case the same sequence may appear
	// as the member of multiple descendents.
	// deduplicate will remove duplicate sequences, in favour of those closest in the hierarchy to the supplied alignment, 
	// breaking ties by sorting on alignment name.
	
	public static List<AlignmentMember> listMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Boolean deduplicate, Optional<Expression> whereClause) {
		Expression matchAlignmentOrDescendent = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName());
		
		Map<String, Integer> alignmentNameToDecOrder = new LinkedHashMap<String, Integer>();
		alignmentNameToDecOrder.put(alignment.getName(), 0);
		
		int decOrder = 1;
		if(recursive) {
			List<Alignment> descendents = alignment.getDescendents();
			for(Alignment descAlignment: descendents) {
				String descName = descAlignment.getName();
				alignmentNameToDecOrder.put(descName, decOrder);
				decOrder++;
				matchAlignmentOrDescendent = matchAlignmentOrDescendent.orExp(
						ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, descName));
			}
		}
		
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, whereClause.get().andExp(matchAlignmentOrDescendent));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, matchAlignmentOrDescendent);
		}
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

	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
		public Completer() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return getMemberFieldNames(cmdContext).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
				}
				protected List<String> getMemberFieldNames(ConsoleCommandContext cmdContext) {
					return getProject(cmdContext).getListableMemberFields();
				}
				private Project getProject(ConsoleCommandContext cmdContext) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					Project project = insideProjectMode.getProject();
					return project;
				}
			});
		}
	}
	
}
