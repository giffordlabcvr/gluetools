package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class MemberModeCommand<R extends CommandResult> extends AlignmentModeCommand<R> {


	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	protected String getSourceName() {
		return sourceName;
	}

	protected String getSequenceID() {
		return sequenceID;
	}


	protected MemberMode getMemberMode(CommandContext cmdContext) {
		MemberMode memberMode = (MemberMode) cmdContext.peekCommandMode();
		return memberMode;
	}

	protected Sequence lookupSequence(CommandContext cmdContext) {
		Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class,
				Sequence.pkMap(getSourceName(), getSequenceID()));
		return sequence;
	}
	
}
