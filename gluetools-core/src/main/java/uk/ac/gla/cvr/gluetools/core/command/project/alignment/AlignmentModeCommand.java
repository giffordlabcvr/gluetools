package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AlignmentModeCommand<R extends CommandResult> extends Command<R> {

	public static final String ALIGNMENT_NAME = "alignmentName";


	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	protected static AlignmentMode getAlignmentMode(CommandContext cmdContext) {
		return (AlignmentMode) cmdContext.peekCommandMode();
	}

	protected Alignment lookupAlignment(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
	}
	
	protected static abstract class AlignmentMemberCompleter extends AdvancedCmdCompleter {
		public AlignmentMemberCompleter() {
			super();
			registerVariableInstantiator("sourceName", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SOURCE_NAME_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
						}
			});
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(AlignmentMember.class, AlignmentMember.SEQUENCE_ID_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							qualifierValues.put(AlignmentMember.ALIGNMENT_NAME_PATH, ((AlignmentMode) cmdMode).getAlignmentName());
							qualifierValues.put(AlignmentMember.SOURCE_NAME_PATH, bindings.get("sourceName"));
						}
			});
		}

	}
	
	
}
