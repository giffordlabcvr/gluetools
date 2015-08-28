package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"remove", "segment"}, 
	docoptUsages={"<refStart> <refEnd> <memberStart> <memberEnd>", 
			"-a"},
	docoptOptions={"-a, --allSegments  Remove all segments"},
	metaTags={CmdMeta.updatesDatabase},
	description="Remove a specific aligned segment or all of them", 
	furtherHelp="") 
public class RemoveAlignedSegmentCommand extends MemberModeCommand<DeleteResult> {

	public static final String ALL_SEGMENTS = "allSegments";
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String MEMBER_START = "memberStart";
	public static final String MEMBER_END = "memberEnd";
	
	private Optional<Integer> refStart;
	private Optional<Integer> refEnd;
	private Optional<Integer> memberStart;
	private Optional<Integer> memberEnd;
	private Boolean allSegments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, REF_START, false));
		refEnd = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, REF_END, false));
		memberStart = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MEMBER_START, false));
		memberEnd = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MEMBER_END, false));
		allSegments = PluginUtils.configureBooleanProperty(configElem, ALL_SEGMENTS, true);
		if( !(
				(refStart.isPresent() && refEnd.isPresent() && memberStart.isPresent() && memberEnd.isPresent() && !allSegments) || 
				(!refStart.isPresent() && !refEnd.isPresent() && !memberStart.isPresent() && !memberEnd.isPresent() && allSegments)
				)) {
			usageError();
		}
		
	}
	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either allSegments or both reference start/end and member start/end must be specified");
	}
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		if(allSegments) {
			Expression allMemberSegments = 
					ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName())
					.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()))
					.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
			List<AlignedSegment> segmentsToDelete = GlueDataObject.query(objContext, AlignedSegment.class, 
					new SelectQuery(AlignedSegment.class, allMemberSegments));
			int numDeleted = 0;
			for(AlignedSegment segment: segmentsToDelete) {
				DeleteResult result = GlueDataObject.delete(objContext, AlignedSegment.class, segment.pkMap(), true);
				numDeleted = numDeleted+result.getNumber();
			}
			cmdContext.commit();
			return new DeleteResult(AlignedSegment.class, numDeleted);
		} else {		
			DeleteResult result = GlueDataObject.delete(objContext, AlignedSegment.class, 
					AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), 
							refStart.get(), 
							refEnd.get(), 
							memberStart.get(), 
							memberEnd.get()), true);
			cmdContext.commit();
			return result;
		}
	}

}
