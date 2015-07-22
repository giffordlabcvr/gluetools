package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"remove", "segment"}, 
	docoptUsages={"<refStart> <refEnd> <memberStart> <memberEnd>"},
	description="Remove an aligned segment", 
	furtherHelp="") 
public class RemoveAlignedSegmentCommand extends MemberModeCommand {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String MEMBER_START = "memberStart";
	public static final String MEMBER_END = "memberEnd";
	
	private int refStart;
	private int refEnd;
	private int memberStart;
	private int memberEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
		memberStart = PluginUtils.configureIntProperty(configElem, MEMBER_START, true);
		memberEnd = PluginUtils.configureIntProperty(configElem, MEMBER_END, true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		return GlueDataObject.delete(objContext, AlignedSegment.class, 
				AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), refStart, refEnd, memberStart, memberEnd));
	}

}
