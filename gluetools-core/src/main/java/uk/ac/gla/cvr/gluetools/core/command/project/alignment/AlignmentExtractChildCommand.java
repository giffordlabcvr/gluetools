package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateAlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.CreateReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"extract", "child"},
		docoptUsages={
				"<childAlmtName> ( -m <refSource> <refSequenceId> | -r <refName> )",
		},
		docoptOptions={
				"-r <refName>, --refName <refName>  Use a specific existing reference",
				"-m, --member                       Use a specific alignment member"
		},
		metaTags={CmdMeta.updatesDatabase},
		description="Create new child alignment, extracting its reference and members",
		furtherHelp=
		"If <refName> is specified, this names the reference of the new child. It must also "+
		"be a member of this alignment. If <refName> is not specified, "+
		"this alignment should have a member specified by <refSource> <refSequenceId>. "+
		"In this case, a new reference sequence is created based on this member, and this reference is named <childAlmtName>."+
		"Then, a new child alignment is created, constrained to the specified/new reference, the alignment is named "+
		"<childAlmtName>. Its parent is set to be this alignment. "
	) 
public class AlignmentExtractChildCommand extends AlignmentModeCommand<CreateResult> {

	public static String CHILD_ALMT_NAME = "childAlmtName";
	public static String REF_NAME = "refName";
	public static String REF_SOURCE = "refSource";
	public static String MEMBER = "member";
	public static String REF_SEQUENCE_ID = "refSequenceId";
	
	
	private String childAlmtName;
	private String refName;
	private String refSource;
	private String refSequenceId;
	private boolean member;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		childAlmtName = PluginUtils.configureStringProperty(configElem, CHILD_ALMT_NAME, true);
		member = PluginUtils.configureBooleanProperty(configElem, MEMBER, true);
		refName = PluginUtils.configureStringProperty(configElem, REF_NAME, false);
		refSource = PluginUtils.configureStringProperty(configElem, REF_SOURCE, false);
		refSequenceId = PluginUtils.configureStringProperty(configElem, REF_SEQUENCE_ID, false);
		if(!(
				(refSource != null && refSequenceId != null && member && refName == null) ||
				(refSource == null && refSequenceId == null && !member && refName != null)
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either <refName> or both <refSource> and <refSequenceId> must be specified");
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Alignment thisAlignment = lookupAlignment(cmdContext);
		ReferenceSequence childRefSeq;
		if(refName == null) {
			AlignmentMember childRefMember = GlueDataObject.lookup(cmdContext, 
					AlignmentMember.class, AlignmentMember.pkMap(thisAlignment.getName(), refSource, refSequenceId));
			childRefSeq = CreateReferenceSequenceCommand.createRefSequence(cmdContext, childAlmtName, childRefMember.getSequence());
		} else {
			childRefSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refName));
			Sequence refSequence = childRefSeq.getSequence();
			// check named reference is indeed a member of this alignment.
			GlueDataObject.lookup(cmdContext, 
					AlignmentMember.class, AlignmentMember.pkMap(thisAlignment.getName(), 
							refSequence.getSource().getName(), 
							refSequence.getSequenceID()));
		}
		CreateAlignmentCommand.createAlignment(cmdContext, childAlmtName, childRefSeq, thisAlignment);
		cmdContext.commit();
		return new CreateResult(Alignment.class, 1);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("refName", 
					new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					AlignmentMode almtMode = (AlignmentMode) cmdContext.peekCommandMode();
					SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, 
							ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, almtMode.getAlignmentName()));
					List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
					Set<String> refNameSuggestions = new LinkedHashSet<String>();
					for(AlignmentMember almtMember: almtMembers) {
						refNameSuggestions.addAll(almtMember.getSequence().getReferenceSequences().stream()
								.map(ref -> ref.getName()).collect(Collectors.toList()));
					}
					return refNameSuggestions.stream().map(refName -> new CompletionSuggestion(refName, true)).collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("refSource", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SOURCE_NAME_PATH) {
				@SuppressWarnings("rawtypes")
				@Override
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings,
						Map<String, Object> qualifierValues) {
					qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
				}
			});
			registerVariableInstantiator("refSequenceId", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SEQUENCE_ID_PATH) {
				@SuppressWarnings("rawtypes")
				@Override
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings,
						Map<String, Object> qualifierValues) {
					qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
					qualifierValues.put(AlignmentMember.SOURCE_NAME_PATH, bindings.get("refSource"));
				}
			});

		}

	}
	
}
