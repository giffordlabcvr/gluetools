package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.memberFLocNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class MemberFLocNoteModeCommand<R extends CommandResult> extends MemberModeCommand<R> {

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

	protected String getFeatureName() {
		return featureName;
	}

	protected String getRefSeqName() {
		return refSeqName;
	}

	protected static MemberFLocNoteMode getFLocNoteMode(CommandContext cmdContext) {
		return (MemberFLocNoteMode) cmdContext.peekCommandMode();
	}

	protected MemberFLocNote lookupMemberFLocNote(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, MemberFLocNote.class, 
				MemberFLocNote.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), getRefSeqName(), getFeatureName()));
	}
	
	
}
