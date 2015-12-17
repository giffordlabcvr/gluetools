package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.groupMember.GroupMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


public abstract class GroupModeCommand<R extends CommandResult> extends Command<R> {

	public static final String GROUP_NAME = "groupName";


	private String groupName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		groupName = PluginUtils.configureStringProperty(configElem, GROUP_NAME, true);
	}

	protected String getGroupName() {
		return groupName;
	}

	protected static GroupMode getGroupMode(CommandContext cmdContext) {
		return (GroupMode) cmdContext.peekCommandMode();
	}

	protected SequenceGroup lookupGroup(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, SequenceGroup.class, 
				SequenceGroup.pkMap(getGroupName()));
	}
	
	protected static abstract class GroupMemberCompleter extends AdvancedCmdCompleter {
		public GroupMemberCompleter() {
			super();
			registerVariableInstantiator("sourceName", 
					new QualifiedDataObjectNameInstantiator(GroupMember.class, GroupMember.SOURCE_NAME_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(GroupMember.GROUP_NAME_PATH, ((GroupMode) cmdMode).getGroupName());
						}
			});
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(GroupMember.class, GroupMember.SEQUENCE_ID_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(GroupMember.GROUP_NAME_PATH, ((GroupMode) cmdMode).getGroupName());
							qualifierValues.put(GroupMember.SOURCE_NAME_PATH, bindings.get("sourceName"));
						}
			});
		}

	}
	
	
}
