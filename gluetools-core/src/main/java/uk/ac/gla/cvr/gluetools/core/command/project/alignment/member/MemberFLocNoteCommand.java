package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.memberFLocNote.MemberFLocNoteMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"member-floc-note"},
	docoptUsages={"<refSeqName> <featureName>"},
	description="Enter command mode for a member-featureLoc note") 
@EnterModeCommandClass(commandModeClass = MemberFLocNoteMode.class)
public class MemberFLocNoteCommand extends MemberModeCommand<OkResult>  {

	public static final String REF_SEQ_NAME = "refSeqName";
	public static final String FEATURE_NAME = "featureName";
	private String refSeqName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		GlueDataObject.lookup(cmdContext, MemberFLocNote.class, 
				MemberFLocNote.pkMap(getAlignmentName(), getSourceName(), getSequenceID(),
				refSeqName, featureName));
		cmdContext
			.pushCommandMode(new MemberFLocNoteMode(getMemberMode(cmdContext).getProject(), 
					this,
					getAlignmentName(), getSourceName(), getSequenceID(),
					refSeqName, featureName));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("refSeqName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					MemberMode memberMode = (MemberMode) cmdContext.peekCommandMode();
					List<MemberFLocNote> membFLocNotes = memberMode.getAlignmentMember(cmdContext).getFLocNotes();
					Set<String> refNamesSet = membFLocNotes.stream()
							.map(mfln -> mfln.getFeatureLoc().getReferenceSequence().getName())
							.collect(Collectors.toSet());
					return refNamesSet.stream().map(rn -> new CompletionSuggestion(rn, true)).collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String refSeqName = (String) bindings.get("refSeqName");
					if(refSeqName != null) {
						MemberMode memberMode = (MemberMode) cmdContext.peekCommandMode();
						List<MemberFLocNote> membFLocNotes = memberMode.getAlignmentMember(cmdContext).getFLocNotes();
						return membFLocNotes.stream()
								.filter(mfln -> mfln.getFeatureLoc().getReferenceSequence().getName().equals(refSeqName))
								.map(mfln -> new CompletionSuggestion(mfln.getFeatureLoc().getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}
	}
	

}